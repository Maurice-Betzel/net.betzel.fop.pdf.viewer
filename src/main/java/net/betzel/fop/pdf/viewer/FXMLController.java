package net.betzel.fop.pdf.viewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javax.imageio.ImageIO;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.PageSequenceResults;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.xmlgraphics.util.MimeConstants;

public class FXMLController implements Initializable {

    private final ObservableList<BufferedImage> images = FXCollections.observableArrayList();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService backgoundExecutor = Executors.newSingleThreadExecutor();
    private ObjectProperty<ImageView> imageViewObjectProperty;
    private TransformerFactory transformerFactory;
    private ScheduledFuture<?> refresherHandle;
    private ScrollPane scrollPane;
    private FopFactory fopFactory;
    private File fopConfig;
    private File xmlFile;
    private File xslFile;
    private volatile boolean isReady = false;

    @FXML
    private Pagination paginationCenter;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scrollPane = new ScrollPane();
        scrollPane.setPannable(true);
        imageViewObjectProperty = new SimpleObjectProperty<>();
        scrollPane.contentProperty().bind(imageViewObjectProperty);
        images.addListener(new ListChangeListener<BufferedImage>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends BufferedImage> c) {
                if (images.isEmpty()) {
                    paginationCenter.setCurrentPageIndex(0);
                }
            }
        });
        paginationCenter.pageCountProperty().bind(new IntegerBinding() {
            {
                super.bind(images);
            }

            @Override
            protected int computeValue() {
                return images.isEmpty() ? Pagination.INDETERMINATE : images.size();
            }
        });
        paginationCenter.disableProperty().bind(Bindings.isEmpty(images));
        paginationCenter.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageNumber) {
                if (images.isEmpty()) {
                    return null;
                } else if (pageNumber >= images.size() || pageNumber < 0) {
                    return null;
                } else {
                    updateImage(pageNumber);
                    return scrollPane;
                }
            }
        });
    }

    private void createImages(FileStreamSources fileStreamSources) {
        final Task<List<BufferedImage>> createImagesTask = new Task<List<BufferedImage>>() {
            @Override
            protected List<BufferedImage> call() throws Exception {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                List<BufferedImage> bufferedImages = new ArrayList();
                FOUserAgent userAgent = fopFactory.newFOUserAgent();
                Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, byteArrayOutputStream);
                Transformer transformer = transformerFactory.newTransformer(fileStreamSources.getXslSource());
                transformer.setErrorListener(new XmlTransformErrorListener());
                Result result = new SAXResult(fop.getDefaultHandler());
                transformer.transform(fileStreamSources.getXmlSource(), result);
                FormattingResults foResults = fop.getResults();
                List pageSequences = foResults.getPageSequences();
                for (java.util.Iterator it = pageSequences.iterator(); it.hasNext();) {
                    PageSequenceResults pageSequenceResults = (PageSequenceResults) it.next();
                    System.out.println("PageSequence "
                            + (String.valueOf(pageSequenceResults.getID()).length() > 0 ? pageSequenceResults.getID() : "<no id>")
                            + " generated " + pageSequenceResults.getPageCount() + " pages.");
                }
                PDDocument pdDocument = null;
                try{
                    pdDocument = PDDocument.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                    PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
                    int pageCounter = 0;
                    for (PDPage pdPage : pdDocument.getPages()) {
                        bufferedImages.add(pdfRenderer.renderImageWithDPI(pageCounter, 150, ImageType.RGB));
                        pageCounter++;
                    }
                } finally {
                    if (pdDocument != null) {
                        try {
                            pdDocument.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                return bufferedImages;
            }
        };
        createImagesTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                Platform.runLater(() -> {
                    images.clear();
                    images.addAll(createImagesTask.getValue());
                    System.out.println("Fertig imaging " + images.size());
                });
            }
        });
        createImagesTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                System.out.println("Fehler imaging! " + createImagesTask.getException().toString());
            }
        });
        backgoundExecutor.submit(createImagesTask);
    }

    private void updateImage(final int pageNumber) {
        //BufferedImage bufferedImage = images.get(pageNumber);
        final Task<ImageView> updateImageTask = new Task<ImageView>() {
            @Override
            protected ImageView call() throws Exception {
                BufferedImage bufferedImage = images.get(pageNumber);
                ByteArrayOutputStream fbaos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", fbaos);
                ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(fbaos.toByteArray()), bufferedImage.getWidth() / 2, bufferedImage.getHeight() / 2, true, true));
                imageView.setPreserveRatio(true);
                return imageView;
            }
        };
        updateImageTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                Platform.runLater(() -> {
                    imageViewObjectProperty.set(updateImageTask.getValue());
                });
            }
        });
        updateImageTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                System.out.println("Fehler beim updaten! " + updateImageTask.getException().toString());
            }
        });
        backgoundExecutor.submit(updateImageTask);
    }

    @FXML
    private void refresh() {
        if (isReady) {
            String xml = null;
            String xsl = null;
            try {
                xml = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
                xsl = new String(Files.readAllBytes(xslFile.toPath()), StandardCharsets.UTF_8);
                createImages(new FileStreamSources(xml, xsl));
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        } else {
            System.err.println("NOT READY!");
        }
    }

    @FXML
    public void fopConfigFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("FOP Configuration file (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        fopConfig = fileChooser.showOpenDialog(MainApp.getPrimaryStage());
        // FOP
        try {
            transformerFactory = new net.sf.saxon.TransformerFactoryImpl();
            FopConfParser parser = new FopConfParser(fopConfig);
            FopFactoryBuilder builder = parser.getFopFactoryBuilder();
            fopFactory = builder.build();
            isReady();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @FXML
    public void xmlFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML file (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        xmlFile = fileChooser.showOpenDialog(MainApp.getPrimaryStage());
        isReady();
    }

    @FXML
    public void xslFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML file (*.xsl)", "*.xsl");
        fileChooser.getExtensionFilters().add(extFilter);
        xslFile = fileChooser.showOpenDialog(MainApp.getPrimaryStage());
        isReady();
    }

    @FXML
    public void startAutoUpdate() {
        Runnable refresher = new Runnable() {
            public void run() {
                refresh();
            }
        };
        refresherHandle = scheduledExecutor.scheduleAtFixedRate(refresher, 0, 10, TimeUnit.SECONDS);
    }

    @FXML
    public void stopAutoUpdate() {
        scheduledExecutor.schedule(new Runnable() {
            public void run() {
                refresherHandle.cancel(true);
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void isReady() {
        if (xmlFile != null && xslFile != null && fopConfig != null) {
            isReady = true;
        } else {
            isReady = false;
        }
    }

    public void shutDown() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
        backgoundExecutor.shutdown();
        images.clear();
        imageViewObjectProperty.unbind();
    }

}