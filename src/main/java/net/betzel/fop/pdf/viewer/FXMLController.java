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
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javax.imageio.ImageIO;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import net.betzel.fop.pdf.viewer.FileChangeWatcher.FileChange;
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

public class FXMLController implements Initializable, FileChange {

    private final FileChangeWatcher fileChangeWatcher = new FileChangeWatcher(1000, this, this.getClass().getCanonicalName());
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final XmlTransformErrorListener xmlTransformErrorListener = new XmlTransformErrorListener();
    private final ObservableList<BufferedImage> images = FXCollections.observableArrayList();
    private final ExecutorService backgoundExecutor = Executors.newSingleThreadExecutor();
    private final ProgressDialog scanProgressDialog = new ProgressDialog();
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private ObjectProperty<ImageView> imageViewObjectProperty;
    private TransformerFactory transformerFactory;
    private ScheduledFuture<?> refresherHandle;
    private FopEventListener fopEventListener;
    private ScrollPane scrollPane;
    private FopFactory fopFactory;
    private DoubleProperty zoom;
    private File fopConfig;
    private File xmlFile;
    private File xslFile;

    private volatile boolean isReady = false;

    @FXML
    private TextArea fopEvents;

    @FXML
    private Pagination paginationCenter;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fopEventListener = new FopEventListener(fopEvents);
        fopEvents.setEditable(false);
        fopEvents.textProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                fopEvents.setScrollTop(Double.MAX_VALUE);
            }
        });
        scrollPane = new ScrollPane();
        scrollPane.setPannable(true);
        zoom = new SimpleDoubleProperty(1);
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
        ChangeListener<Number> changeListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (newValue.floatValue() < 0.10 || newValue.floatValue() > 1.75) {
                    zoom.set(oldValue.doubleValue());
                } else if (!images.isEmpty()) {
                    updateImage(paginationCenter.getCurrentPageIndex());
                }
            }
        };
        zoom.addListener(changeListener);
        scrollPane.addEventFilter(ScrollEvent.ANY, (ScrollEvent event) -> {
            if (event.getDeltaY() > 0) {
                zoom.set(zoom.get() * 1.15);
            } else if (event.getDeltaY() < 0) {
                zoom.set(zoom.get() / 1.15);
            }
        });
        fileChangeWatcher.start();
    }

    private void createImages(FileStreamSources fileStreamSources) {
        Platform.runLater(() -> {
            scanProgressDialog.show();
        });
        final Task<List<BufferedImage>> createImagesTask = new Task<List<BufferedImage>>() {
            @Override
            protected List<BufferedImage> call() throws Exception {

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                List<BufferedImage> bufferedImages = new ArrayList();
                FOUserAgent userAgent = fopFactory.newFOUserAgent();
                userAgent.getEventBroadcaster().addEventListener(fopEventListener);
                Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, byteArrayOutputStream);
                Transformer transformer = transformerFactory.newTransformer(fileStreamSources.getXslSource());
                transformer.setErrorListener(xmlTransformErrorListener);
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
                try (PDDocument pdDocument = PDDocument.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
                    PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
                    int pageCounter = 0;
                    for (PDPage pdPage : pdDocument.getPages()) {
                        bufferedImages.add(pdfRenderer.renderImageWithDPI(pageCounter, 150, ImageType.RGB));
                        pageCounter++;
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
                });
            }
        });
        createImagesTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                System.err.println("Fehler imaging! " + createImagesTask.getException().toString());
                Platform.runLater(() -> {
                    scanProgressDialog.close();
                    if (reentrantLock.isLocked()) {
                        reentrantLock.unlock();
                    }
                });
            }
        });
        backgoundExecutor.submit(createImagesTask);
    }

    private void updateImage(final int pageNumber) {
        final Task<ImageView> updateImageTask = new Task<ImageView>() {
            @Override
            protected ImageView call() throws Exception {
                BufferedImage bufferedImage = images.get(pageNumber);
                final int scaledWidth = (int) (bufferedImage.getWidth() * zoom.get());
                final int scaledHeight = (int) (bufferedImage.getHeight() * zoom.get());
                ByteArrayOutputStream fbaos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", fbaos);
                ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(fbaos.toByteArray()), scaledWidth, scaledHeight, true, true));
                imageView.setPreserveRatio(true);
                return imageView;
            }
        };
        updateImageTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                Platform.runLater(() -> {
                    imageViewObjectProperty.set(updateImageTask.getValue());
                    scanProgressDialog.close();
                    if (reentrantLock.isLocked()) {
                        reentrantLock.unlock();
                    }
                });
            }
        });
        updateImageTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                System.err.println("Fehler beim updaten! " + updateImageTask.getException().toString());
                Platform.runLater(() -> {
                    scanProgressDialog.close();
                    if (reentrantLock.isLocked()) {
                        reentrantLock.unlock();
                    }
                });
            }
        });
        backgoundExecutor.submit(updateImageTask);
    }

    @FXML
    @Override
    public void changed() {
        if (isReady) {
            if (reentrantLock.isLocked()) {
                fopEvents.appendText("Work in progress\n");
            } else {
                try {
                    reentrantLock.lock();
                    String xml = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
                    String xsl = new String(Files.readAllBytes(xslFile.toPath()), StandardCharsets.UTF_8);
                    createImages(new FileStreamSources(xml, xsl));
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        } else {
            System.out.println("NOT READY!");
            fopEvents.appendText("Missing files\n");
        }
    }

    @FXML
    public void fopConfigFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("FOP Configuration file (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        fopConfig = fileChooser.showOpenDialog(MainApp.getPrimaryStage());
        if (fopConfig != null) {
            try {
                transformerFactory = new net.sf.saxon.TransformerFactoryImpl();
                FopConfParser parser = new FopConfParser(fopConfig);
                FopFactoryBuilder builder = parser.getFopFactoryBuilder();
                fopFactory = builder.build();
                isReady();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    @FXML
    public void xmlFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML file (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        xmlFile = fileChooser.showOpenDialog(MainApp.getPrimaryStage());
        if (xmlFile != null) {
            fileChangeWatcher.addTarget(xmlFile.toPath());
            isReady();
        }
    }

    @FXML
    public void xslFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML file (*.xsl)", "*.xsl");
        fileChooser.getExtensionFilters().add(extFilter);
        xslFile = fileChooser.showOpenDialog(MainApp.getPrimaryStage());
        if (xslFile != null) {
            fileChangeWatcher.addTarget(xslFile.toPath());
            isReady();
        }
    }

    @FXML
    public void startAutoUpdate() {
        Runnable refresher = new Runnable() {
            public void run() {
                changed();
            }
        };
        refresherHandle = scheduledExecutor.scheduleAtFixedRate(refresher, 0, 15, TimeUnit.SECONDS);
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
            changed();
        } else {
            isReady = false;
        }
    }

    public void shutDown() {
        fileChangeWatcher.stop();
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
        backgoundExecutor.shutdown();
        images.clear();
        imageViewObjectProperty.unbind();
    }

}
