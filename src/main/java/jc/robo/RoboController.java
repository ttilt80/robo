/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jc.robo;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.poi.POITextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.html.HTMLAnchorElement;
import org.w3c.dom.html.HTMLDivElement;
import org.w3c.dom.html.HTMLInputElement;
import org.w3c.dom.html.HTMLSelectElement;

/**
 * FXML Controller class
 *
 * @author João Carlos
 */
public class RoboController implements Initializable {

    final String trf = "http://www2.trf4.jus.br/trf4/",
            tj = "http://esaj.tjsc.jus.br/cpopg/open.do",
            tj2g = "http://www.tjsc.jus.br/consulta-tribunal-de-justica";
    final ObservableList<CPF> cpfsValidos = FXCollections.observableArrayList();
    final ObservableList<CPF> todosCpfs = FXCollections.observableArrayList();
    WebView web = new WebView(); //for debugging (shows the actual web page in separate stage)
    final WebEngine tjEng = web.getEngine();
    final WebEngine tj2gEng = new WebView().getEngine();
    final WebEngine trfEng = new WebView().getEngine();
    XPathExpression expTjComarca, expTjPesquisaPor, expTjDocParte, expTjNadaConsta, expTJConsta1, expTJConsta2, expTrfCaptchaTxtField, expTj2gPesquisarPor, expTj2gDocumento, expTj2gNadaConsta, expTrfCaptchaSrc;
    final SimpleBooleanProperty tjInicio = new SimpleBooleanProperty(true);
    final SimpleBooleanProperty tj2gInicio = new SimpleBooleanProperty(true);
    final SimpleBooleanProperty trfInicio = new SimpleBooleanProperty(true);
    static final Logger LOGGER = Logger.getLogger(RoboController.class.getName());
    final Dialog<ButtonType> apagarCpfsDialog = new Alert(AlertType.CONFIRMATION, "Tem certeza que deseja apagar todos os CPFs?");
    final ObservableList<File> listaDeArqs = FXCollections.observableArrayList();
    final Set<String> cpfsDeArquivo = new LinkedHashSet<>();

    @FXML
    ImageView tjImage;
    @FXML
    ImageView trfImage;
    @FXML
    TextField tjCaptchaField;
    @FXML
    TextField trfCaptchaField;
    @FXML
    VBox trfCaptchaVbox;
    @FXML
    VBox tjCaptchaVbox;
    @FXML
    GridPane gridpane;
    @FXML
    Label avisos;
    @FXML
    Button pesquisar;
    @FXML
    ProgressIndicator tjProgress;
    @FXML
    ProgressIndicator tj2gProgress;
    @FXML
    ProgressIndicator trfProgress;
    @FXML
    TableView<File> tabelaArqs;
    @FXML
    TableColumn<File, String> colunaArq;
    @FXML
    TableColumn<File, Number> colunaCPFs;
    @FXML
    TabPane tabPane;
    @FXML
    Button transferirCPFs;

    @FXML
    void pesquisar(ActionEvent e) {
        cpfsValidos.clear();
        todosCpfs.forEach((cpf) -> {
            if (cpf.estaVazio()) {
                cpf.apagaLinha();
            } else if (!cpf.cpfValido()) {
                cpf.marcaInvalido();
            } else {
                cpfsValidos.add(cpf);
            }
        });
        if (cpfsValidos.isEmpty()) {
            avisos.setText("Insira algum CPF válido");
            return;
        }

        avisos.setText("<< Brought to you by JC >>");

        tjEng.load(tj);
        tjProgress.setProgress(-1);
        LOGGER.log(Level.FINE, "TJ: Carregando 1a página");

        tj2gEng.load(tj2g);
        tj2gProgress.setProgress(-1);
        LOGGER.log(Level.FINE, "TJ2g: Carregando 1a página");

        trfEng.load(trf);
        trfProgress.setProgress(-1);
        LOGGER.log(Level.FINE, "TRF: Carregando 1a página");
    }

    @FXML
    void maisCpfs(ActionEvent e) {
        TextField cpfField;
        Hyperlink[] links = new Hyperlink[Tribunal.values().length];
        for (int i = 0; i < links.length; i++) {
            links[i] = new Hyperlink();
            links[i].setFocusTraversable(false);
        }
        gridpane.addRow(todosCpfs.size(), cpfField = new TextField(), links[0], links[1], links[2]);
        cpfField.setPromptText("CPF");

        todosCpfs.add(new CPF(cpfField, links));

        if (System.getProperty("debug") != null) {
            switch (GridPane.getRowIndex(cpfField)) {
                case 0:
                    cpfField.setText("60165146915");//TJ: CONSTA TJ2g: CONSTA TRF: NADA CONSTA
                    break;
                case 2:
                    cpfField.setText("144.880.609-78");//TJ: CONSTA TJ2g: NADA CONSTA TRF: CONSTA
                    break;
                case 4:
                    cpfField.setText("098.282.304-53");//TJ: NADA CONSTA TJ2g: NADA CONSTA TRF: NADA CONSTA
                    break;
                case 8:
                    cpfField.setText("96227966991");//TJ: NADA CONSTA TJ2g: CONSTA TRF: NADA CONSTA
                    break;
                case 9:
                    cpfField.setText("18622224104");//TJ: CONSTA TJ2g: NADA CONSTA TRF: NADA CONSTA
                    break;
            }
        }
    }

    @FXML
    void apagaCpfs(ActionEvent e) {
        apagarCpfsDialog.showAndWait().filter(result -> result == ButtonType.OK)
                .ifPresent(result -> {
                    todosCpfs.forEach(CPF::apagaLinha);
                    listaDeArqs.clear();
                    cpfsDeArquivo.clear();
                });
    }

    @FXML
    void continuarTrf(ActionEvent e) {
        if (trfCaptchaField.getText().isEmpty()) {
            avisos.setText("Preencha o captcha");
            return;
        }
        try {
            Document doc = trfEng.getDocument();
            HTMLInputElement el;
            (el = (HTMLInputElement) expTrfCaptchaTxtField.evaluate(doc, XPathConstants.NODE)).setValue(trfCaptchaField.getText());
            trfCaptchaVbox.setVisible(false);
            el.getForm().submit();
            LOGGER.log(Level.FINE, "TRF > Form submetido com captcha");
        } catch (XPathExpressionException ex) {
            avisos.setText("Consertar xpath");
        }
    }

    @FXML
    void continuarTj(ActionEvent e) {
        if (tjCaptchaField.getText().isEmpty()) {
            avisos.setText("Preencha o captcha");
            return;
        }

        Document doc = tjEng.getDocument();
        HTMLInputElement captchaField;
        (captchaField = (HTMLInputElement) doc.getElementById("defaultCaptchaCampo")).setValue(tjCaptchaField.getText());
        tjCaptchaVbox.setVisible(false);
        captchaField.getForm().submit();
        LOGGER.log(Level.FINE, "TJ > Form submetido com captcha");
    }

    @FXML
    void arrastarArquivos(DragEvent e) {
        Dragboard db = e.getDragboard();
        if (db.hasFiles()) {
            e.acceptTransferModes(TransferMode.LINK);
            tabPane.getSelectionModel().select(1);
        }
        e.consume();
    }

    @FXML
    void soltarArquivos(DragEvent e) {
        Dragboard db = e.getDragboard();
        if (db.hasFiles()) {
            listaDeArqs.addAll(db.getFiles());
            e.setDropCompleted(true);
        }
        e.consume();
    }

    @FXML
    void transferirCPFs(ActionEvent e) {
        todosCpfs.forEach(CPF::apagaLinha);
        int excessCpfs = cpfsDeArquivo.size() - todosCpfs.size();

        while (excessCpfs > 0) {
            maisCpfs(null);
            excessCpfs--;
        }

        int i = 0;
        for (String cpf : cpfsDeArquivo) {
            todosCpfs.get(i++).preenche(cpf);
        }

        tabPane.getSelectionModel().select(0);
    }

    @FXML
    void escolherArquivos(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Listas de Nomeação", "*.doc", "*.docx"));
        List<File> arqsEscolhidos = fileChooser.showOpenMultipleDialog(tabelaArqs.getScene().getWindow());
        if (arqsEscolhidos != null) {
            listaDeArqs.addAll(arqsEscolhidos);
        }
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (System.getProperty("debug") != null) {
            Scene scene = new Scene(new Group(web));
            Stage webStage = new Stage();
            webStage.setScene(scene);
            webStage.show();

            LOGGER.setLevel(Level.FINE);
            Logger.getLogger("").getHandlers()[0].setLevel(Level.FINE);
        }

        System.setProperty("java.security.auth.login.config", getClass().getResource("/login.conf").toString());
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        //3/2/17: Testei sem isso e definitivamente o padrão é não ter essa propriedade setada (null)
        //e consequentemente a não identificação dos proxies. O sistema acha que a conexão é DIRECT (sem proxy)
        System.setProperty("java.net.useSystemProxies", "true");
        LOGGER.log(Level.FINE, "java.net.useSystemProxies = {0} -- Proxies detected: {1}",
                new Object[]{System.getProperty("java.net.useSystemProxies"),
                    ProxySelector.getDefault().select(URI.create(tj))});

        //prepara o Xpath NamespaceContext já que a implementação não funciona corretamente com uso de default Namespace 
        //22/3/15: Na verdade a implementação Java está correta; ela segue o que a especificação preconiza, que é algo burro:
        //http://www.w3.org/TR/xpath/#node-tests
        //"A QName in the node test is expanded into an expanded-name using the namespace declarations from the
        //expression context. This is the same way expansion is done for element type names in start and end-tags
        //** except that the default namespace declared with xmlns is not used: if the QName does not have a
        //prefix, then the namespace URI is null ** (this is the same way attribute names are expanded)"
        //Isso faz com que seja impossível uma expressão xpath que capture um elemento com default namespace. E por isso é necessário
        //usar a classe NamespaceContext, junto de expressões com um prefixo dummy ("x").
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return "http://www.w3.org/1999/xhtml";
            }

            @Override
            public String getPrefix(String namespaceURI) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        try {
            expTjComarca = xp.compile("//x:select[@name=\"dadosConsulta.localPesquisa.cdLocal\"]");
            expTjPesquisaPor = xp.compile("//x:select[@name=\"cbPesquisa\"]");
            expTjDocParte = xp.compile("//x:tr[@id=\"DOCPARTE\"]//x:input");
            expTjNadaConsta = xp.compile("//x:li[contains(.,\"Não existem informações\")]");
            expTJConsta1 = xp.compile("//x:td[contains(.,\"Dados do processo\")]");
            expTJConsta2 = xp.compile("//x:td[contains(.,\"Resultados\")]");

            expTj2gPesquisarPor = xp.compile("//x:select[@name=\"cbPesquisa\"]");
            expTj2gDocumento = xp.compile("//x:input[@name=\"dePesquisa\"]");
            expTj2gNadaConsta = xp.compile("//x:td[@class=\"errors\"][contains(.,\"Não foi encontrado nenhum processo\")]");

            expTrfCaptchaTxtField = xp.compile("//x:input[@name=\"txtPalavraGerada\"]");
            expTrfCaptchaSrc = xp.compile("//@src[contains(.,\"gera_imagem\")]");
        } catch (XPathExpressionException ex) {
            avisos.setText("consertar xpath");
        }

        for (int i = 0; i <= 9; i++) {
            maisCpfs(null);
        }

        pesquisar.disableProperty().bind((tjInicio.and(tj2gInicio).and(trfInicio)).not());

        tabelaArqs.setItems(listaDeArqs);
        tabelaArqs.setPlaceholder(new Text("Arraste arquivos de lista de nomeação para cá!"));
        colunaArq.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getName()));

        Pattern cpfRegexp = Pattern.compile("CPF: *([-.\\d]+)", Pattern.CASE_INSENSITIVE);
        colunaCPFs.setCellValueFactory(p -> {
            int cpfCount = 0;
            try (POITextExtractor extractor = ExtractorFactory.createExtractor(p.getValue())) {
                String text = extractor.getText();
                Matcher m = cpfRegexp.matcher(text);
                while (m.find()) {
                    cpfsDeArquivo.add(m.group(1));
                    cpfCount++;
                }
            } catch (IOException | java.lang.IllegalArgumentException | OpenXML4JException | XmlException ex) {
                //java.lang.IllegalArgumentException is caught even though it's unchecked because
                //ExtractorFactory.createExtractor may throw it if it received an invalid file (non-OLE2, non-OOXML).
                LOGGER.log(Level.FINE, "Erro lendo arquivo " + p.getValue().getName(), ex);
            }
            return new ReadOnlyIntegerWrapper(cpfCount);
        });
        transferirCPFs.disableProperty().bind((new ReadOnlyListWrapper<>(listaDeArqs)).emptyProperty().
                or(pesquisar.disabledProperty()));

        tjEng.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            ListIterator<CPF> cpfItr;
            CPF cpf;
            Document doc;
            int retries = 0;

            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                if (newValue == Worker.State.FAILED) {
                    avisos.setText("TJ: " + tjEng.getLoadWorker().getException().toString());
                    tjInicio.set(true);
                } else if (newValue == Worker.State.SUCCEEDED) {
                    doc = tjEng.getDocument();
                    try {
                        if (tjInicio.get()) {
                            cpfItr = cpfsValidos.listIterator();
                            cpf = cpfItr.next();
                            LOGGER.log(Level.FINE, "TJ Cpf: {0}", cpf);
                            tjInicio.set(false);
                        }

                        //se tiver algum resultado, processa
                        if (expTjNadaConsta.evaluate(doc, XPathConstants.NODE) != null) {
                            LOGGER.log(Level.FINE, "TJ Cpf: {0} > NADA CONSTA", cpf);
                            cpf.consta(Tribunal.TJ, false, null);
                            tjProgress.setProgress(cpfItr.nextIndex() / Double.valueOf(cpfsValidos.size()));
                            //já pega o próximo cpf, se houver
                            if (cpfItr.hasNext()) {
                                cpf = cpfItr.next();
                                LOGGER.log(Level.FINE, "TJ Cpf: {0}", cpf);
                            } //senão termina por aqui
                            else {
                                LOGGER.log(Level.FINE, "TJ FIM");
                                tjInicio.set(true);
                                return;
                            }
                        } else if (expTJConsta1.evaluate(doc, XPathConstants.NODE) != null
                                || expTJConsta2.evaluate(doc, XPathConstants.NODE) != null) {
                            LOGGER.log(Level.FINE, "TJ Cpf: {0} > CONSTA", cpf);
                            cpf.consta(Tribunal.TJ, true, URI.create(tjEng.getLocation()));
                            tjProgress.setProgress(cpfItr.nextIndex() / Double.valueOf(cpfsValidos.size()));
                            //já pega o próximo cpf, se houver
                            if (cpfItr.hasNext()) {
                                cpf = cpfItr.next();
                                LOGGER.log(Level.FINE, "TJ Cpf: {0}", cpf);
                            } //senão termina por aqui
                            else {
                                tjInicio.set(true);
                                LOGGER.log(Level.FINE, "TJ FIM");
                                return;
                            }
                        }

                        //preenche o DOM com o próximo cpf
                        HTMLInputElement cpfField;
                        ((HTMLSelectElement) expTjComarca.evaluate(doc, XPathConstants.NODE)).setValue("-1");
                        ((HTMLSelectElement) expTjPesquisaPor.evaluate(doc, XPathConstants.NODE)).setValue("DOCPARTE");
                        (cpfField = (HTMLInputElement) expTjDocParte.evaluate(doc, XPathConstants.NODE)).setValue(cpf.toString());
                        LOGGER.log(Level.FINE, "TJ Cpf: {0} > Dom preenchido", cpf);

                        //se tiver captcha mostra captcha e sai
                        HTMLDivElement captcha;
                        if ((captcha = (HTMLDivElement) doc.getElementById("captchaCodigo")) != null) {
                            String styleCss = captcha.getAttribute("style");
                            String captchaUrl = styleCss.substring(styleCss.indexOf("url(") + 4, styleCss.indexOf("timestamp=") + 10);
                            long newTimestamp = Long.parseLong(styleCss.substring(styleCss.indexOf("timestamp=") + 10,
                                    styleCss.indexOf("timestamp=") + 23)) + 1;
                            tjImage.setImage(new Image(captchaUrl + newTimestamp));

                            //tjImage.setImage(new Image(new ByteArrayInputStream(Base64.getDecoder().decode(imageBase64))));
                            tjCaptchaVbox.setVisible(true);
                            tjCaptchaField.clear();
                            if (!trfCaptchaField.isFocused()) {
                                tjCaptchaField.requestFocus();
                            }
                            LOGGER.log(Level.FINE, "TJ Cpf: {0} > Captcha exibido", cpf);
                        } //senão tiver captcha já submete agora
                        else {
                            LOGGER.log(Level.FINE, "TJ Cpf: {0} > Form submetido", cpf);
                            retries = 0;
                            avisos.setText("<< Brought to you by JC >>");
                            cpfField.getForm().submit();
                        }
                    } catch (XPathExpressionException | NumberFormatException ex) {
                        avisos.setText("TJ: " + ex.toString());
                        LOGGER.log(Level.SEVERE, "Exceção no TJ", ex);
                        tjInicio.set(true);
                    } catch (NullPointerException ex) {
                        LOGGER.log(Level.FINE, "TJ Cpf: {0} > Recarregando página...", cpf);
                        avisos.setText(MessageFormat.format("TJ: Recarregando página {0,choice,1#1 vez|1<{0} vezes}.", ++retries));
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ex1) {
                            Logger.getLogger(RoboController.class.getName()).log(Level.FINE, "TJ interrompido.", ex1);
                            return;
                        }
                        tjEng.reload();
                    }
                }
            }
        });

        tj2gEng.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

            ListIterator<CPF> cpfItr;
            CPF cpf;

            @Override
            public void changed(ObservableValue<? extends Worker.State> ov, Worker.State t, Worker.State t1) {
                if (t1 == Worker.State.FAILED) {
                    avisos.setText("TJ 2o Grau: " + tj2gEng.getLoadWorker().getException().toString());
                    tj2gInicio.set(true);
                } else if (t1 == Worker.State.SUCCEEDED) {
                    try {
                        if (tj2gInicio.get()) {
                            cpfItr = cpfsValidos.listIterator();
                            cpf = cpfItr.next();
                            LOGGER.log(Level.FINE, "TJ2g Cpf: {0}", cpf);
                            tj2gInicio.set(false);
                            enviar();
                        } else { //pegar resposta
                            Document doc = tj2gEng.getDocument();

                            if (expTj2gNadaConsta.evaluate(doc, XPathConstants.NODE) != null) {
                                cpf.consta(Tribunal.TJ2G, false, null);
                                LOGGER.log(Level.FINE, "TJ2g Cpf: {0} > NADA CONSTA", cpf);
                            } else {
                                cpf.consta(Tribunal.TJ2G, true, URI.create(tj2gEng.getLocation()));
                                LOGGER.log(Level.FINE, "TJ2g Cpf: {0} > CONSTA", cpf);
                            }
                            tj2gProgress.setProgress(cpfItr.nextIndex() / Double.valueOf(cpfsValidos.size()));
                            if (cpfItr.hasNext()) {
                                cpf = cpfItr.next();
                                LOGGER.log(Level.FINE, "TJ2g Cpf: {0}", cpf);
                                enviar();
                            } else { //terminou a lista; reiniciar inicio caso haja nova pesquisa
                                tj2gInicio.set(true);
                                LOGGER.log(Level.FINE, "TJ2g FIM");
                            }
                        }
                    } catch (XPathExpressionException | NullPointerException ex) {
                        avisos.setText("TJ 2o Grau: " + ex.toString());
                        LOGGER.log(Level.SEVERE, "Exceção no TJ2g", ex);
                        tj2gInicio.set(true);
                    }
                }
            }

            void enviar() throws XPathExpressionException {
                Document doc = tj2gEng.getDocument();
                HTMLInputElement el;
                ((HTMLSelectElement) expTj2gPesquisarPor.evaluate(doc, XPathConstants.NODE)).setValue("DOCPARTE");
                (el = (HTMLInputElement) expTj2gDocumento.evaluate(doc, XPathConstants.NODE)).setValue(cpf.toString());
                el.getForm().submit();
                LOGGER.log(Level.FINE, "TJ2g Cpf: {0} > Form submetido", cpf);
            }
        }
        );

        trfEng.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

            ListIterator<CPF> cpfItr;
            CPF cpf;
            boolean paginaInicial = true;
            int retries = 0;

            @Override
            public void changed(ObservableValue<? extends Worker.State> ov, Worker.State t, Worker.State t1) {
                if (t1 == Worker.State.FAILED) {
                    avisos.setText("TRF4: " + trfEng.getLoadWorker().getException().toString());
                    trfInicio.set(true);
                } else if (t1 == Worker.State.SUCCEEDED) {
                    try {
                        String src;
                        Document doc = trfEng.getDocument();
                        //se tiver captcha
                        if (!(src = expTrfCaptchaSrc.evaluate(doc)).isEmpty()) {
                            trfImage.setImage(new Image(URI.create(trfEng.getLocation()).resolve(src).toString()));
                            LOGGER.log(Level.FINE, "TRF Cpf: {0} > Captcha exibido", cpf);
                            trfCaptchaVbox.setVisible(true);
                            trfCaptchaField.clear();
                            if (!tjCaptchaField.isFocused()) {
                                trfCaptchaField.requestFocus();
                            }
                            //se não tiver captcha
                        } else if (paginaInicial) {
                            if (trfInicio.get()) {
                                cpfItr = cpfsValidos.listIterator();
                                trfInicio.set(false);
                            }
                            cpf = cpfItr.next();
                            LOGGER.log(Level.FINE, "TRF Cpf: {0}", cpf);
                            paginaInicial = false;
                            enviar();
                        } else { //pegar resultados
                            //se nada constar
                            if (((Element) doc.getElementsByTagName("body").item(0)).getAttribute("onload").contains("CPF/CNPJ")) {
                                cpf.consta(Tribunal.TRF, false, null);
                                LOGGER.log(Level.FINE, "TRF Cpf: {0} > NADA CONSTA", cpf);
                                trfProgress.setProgress(cpfItr.nextIndex() / Double.valueOf(cpfsValidos.size()));

                                if (cpfItr.hasNext()) {
                                    cpf = cpfItr.next();
                                    LOGGER.log(Level.FINE, "TRF Cpf: {0}", cpf);
                                    enviar();
                                    return;
                                }
                                //se constar
                            } else {
                                cpf.consta(Tribunal.TRF, true, URI.create(trfEng.getLocation()));
                                LOGGER.log(Level.FINE, "TRF Cpf: {0} > CONSTA", cpf);
                                trfProgress.setProgress(cpfItr.nextIndex() / Double.valueOf(cpfsValidos.size()));

                                //volta p/ página de pesquisa, p/ a próxima pesquisa
                                if (cpfItr.hasNext()) {
                                    paginaInicial = true;
                                    LOGGER.log(Level.FINE, "TRF Cpf: {0} > Voltando p/ página inicial", cpf);
                                    trfEng.load(((HTMLAnchorElement) doc.getElementById("spnNovaConsulta").getElementsByTagName("a").item(0)).getHref());
                                    return;
                                }
                            }
                            //se chegar nesse ponto é porq acabou a lista
                            paginaInicial = true;
                            trfInicio.set(true);
                            LOGGER.log(Level.FINE, "TRF FIM");
                        }
                    } catch (XPathExpressionException | NullPointerException ex) {
                        avisos.setText("TRF4: " + ex.toString());
                        LOGGER.log(Level.SEVERE, "Exceção no TRF", ex);
                        trfInicio.set(true);
                    }
                }
            }

            void enviar() {
                Document doc = trfEng.getDocument();
                ((HTMLSelectElement) doc.getElementById("selForma")).setValue("CP");
                ((HTMLInputElement) doc.getElementById("txtValor")).setValue(cpf.toString());
                ((HTMLInputElement) doc.getElementById("botaoEnviar")).click();
                LOGGER.log(Level.FINE, "TRF Cpf: {0} > Form submetido", cpf);
            }
        });
    }

}

class CPF {

    private final TextField cpf;
    private final Hyperlink[] links;

    public void consta(Tribunal tribunal, boolean consta, URI url) {
        Hyperlink link = links[tribunal.ordinal()];
        if (consta) {
            link.setText("CONSTA");
            link.setTextFill(Color.RED);
            link.setOnAction(event -> {
                try {
                    Desktop.getDesktop().browse(url);
                } catch (IOException ex) {
                    Logger.getLogger(CPF.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } else {
            link.setText("Nada consta");
            link.setTextFill(Color.BLACK);
            link.setOnAction(null);
        }
        link.setUnderline(consta);
    }

    public void preenche(String cpfAPreencher) {
        cpf.setText(cpfAPreencher);
    }

    public boolean estaVazio() {
        return cpf.getText().isEmpty();
    }

    public void apagaLinha() {
        for (Hyperlink link : links) {
            link.setText(null);
            link.setOnAction(null);
        }
        //necessário já que essa função tb é chamada pela RobotController.apagaCpfs()
        cpf.setText("");
    }

    public void marcaInvalido() {
        for (Hyperlink link : links) {
            link.setText("CPF INVÁLIDO");
            link.setTextFill(Color.RED);
            link.setOnAction(null);
            link.setUnderline(false);
        }
    }

    @Override
    public String toString() {
        return cpf.getText().replaceAll("[^0-9]", "");
    }

    public CPF(TextField cpf, Hyperlink[] links) {
        this.cpf = cpf;
        this.links = links;
    }

    private boolean cpfValido(String cpf) {
        if (cpf.length() != 11) {
            return false;
        }
        int dig1, dig2, soma = 0, peso = 10, r;
        for (int i = 0; i <= 8; i++) {
            soma += Integer.parseInt(cpf.substring(i, i + 1)) * peso--;
        }
        r = 11 - (soma % 11);
        dig1 = r == 11 || r == 10 ? 0 : r;

        soma = 0;
        peso = 11;
        for (int i = 0; i <= 8; i++) {
            soma += Integer.parseInt(cpf.substring(i, i + 1)) * peso--;
        }
        soma += dig1 * 2;
        r = 11 - (soma % 11);
        dig2 = r == 11 || r == 10 ? 0 : r;

        return cpf.endsWith(String.valueOf(dig1) + String.valueOf(dig2));
    }

    public boolean cpfValido() {
        return cpfValido(toString());
    }
}

enum Tribunal {
    TJ, TJ2G, TRF;
}
