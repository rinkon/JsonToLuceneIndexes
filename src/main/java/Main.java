import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends Application {

    private Pane root = new Pane();
    private Stage mainStage;
    private List<String> choosenJsonFiles = new ArrayList<>();
    private List<String> indexDestinationFolders = new ArrayList<>();
    private Label textLabel;
    private Label progressLabel;
    private Label progressLabelTwo;
    private ProgressBar progressBar;
    private Button indexButton;
    private volatile int count = 1;
    private volatile long totalLine;

    private String[] allPossibleFields = {"publication_number", "application_number", "country_code", "kind_code", "application_kind", "application_number_formatted", "pct_number", "family_id", "title_localized-->text", "title_localized-->language", "abstract_localized-->text", "abstract_localized-->language", "publication_date", "filing_date", "grant_date", "priority_date", "priority_claim-->publication_number", "priority_claim-->application_number", "priority_claim-->npl_text", "priority_claim-->type", "priority_claim-->category", "priority_claim-->filing_date", "inventor", "inventor_harmonized-->name", "inventor_harmonized-->country_code", "assignee", "assignee_harmonized-->name", "assignee_harmonized-->country_code", "examiner-->name", "examiner-->department", "examiner-->level", "ipc-->code", "ipc-->inventive", "ipc-->first", "cpc-->code", "cpc-->inventive", "cpc-->first", "citation-->publication_number", "citation-->application_number", "citation-->npl_text", "citation-->type", "citation-->category", "citation-->filing_date", "entity_status", "art_unit"};
    private HashMap<String, String> fieldValuePair;
    private Document doc;

    @Override
    public void start(Stage primaryStage) throws Exception{
        mainStage = primaryStage;
        addUIComponents();
        primaryStage.setTitle("JSON To Lucene Index Converter");
        primaryStage.setScene(new Scene(root, 600, 375));
        primaryStage.show();
    }

    private void addUIComponents(){

        VBox vBox = new VBox(10);
        vBox.setPrefWidth(500);

        HBox hBox = new HBox(10);
        vBox.setPadding(new Insets(10, 10, 10, 10));
        hBox.setPrefWidth(500);
        Button chooseDirectoryButton = new Button("Choose JSON Files");
        hBox.getChildren().add(chooseDirectoryButton);
        textLabel = new Label("");
        progressLabel = new Label("");
        progressLabel.setPrefWidth(600);
        progressLabelTwo = new Label("");
        progressLabelTwo.setPrefWidth(600);
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);

        chooseDirectoryButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                getJSONFiles();
                progressLabel.setText("");
            }
        });

        indexButton = new Button("Index JSON Files");
        indexButton.setDisable(true);
        indexButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                progressBar.setVisible(true);
                Thread thread = new Thread() {
                    public void run(){
                        try {
                            for (int i = 0; i < choosenJsonFiles.size(); i++) {
                                indexJSONFiles(i);
                            }
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressLabelTwo.setText("");
                                    progressLabel.setText("");
                                    progressBar.setVisible(false);
                                    indexButton.setDisable(true);
                                    if(choosenJsonFiles.size() == 1) {
                                        textLabel.setText(choosenJsonFiles.size() + " file is indexed. You can choose more files now.");
                                    }
                                    else{
                                        textLabel.setText(choosenJsonFiles.size() + " files are indexed. You can choose more files now.");
                                    }
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        });

        vBox.getChildren().add(hBox);
        vBox.getChildren().add(textLabel);
        vBox.getChildren().add(indexButton);
        vBox.getChildren().add(progressLabelTwo);
        vBox.getChildren().add(progressLabel);
        vBox.getChildren().add(progressBar);
        progressBar.setVisible(false);
        root.getChildren().add(vBox);

    }

    private void getJSONFiles(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        List<File> files = fileChooser.showOpenMultipleDialog(mainStage); //showOpenDialog(mainStage);
        if(files != null) {
            choosenJsonFiles.clear();
            indexDestinationFolders.clear();

            for (int i = 0; i < files.size(); i++) {
                choosenJsonFiles.add(files.get(i).getPath());
                indexDestinationFolders.add(files.get(i).getName().substring(0, files.get(i).getName().length() - 5) + "-Indexed");
            }

            if(choosenJsonFiles.size() == 1){
                textLabel.setText(choosenJsonFiles.size() + " file is selected");
            }
            else{
                textLabel.setText(choosenJsonFiles.size() + " files are selected");
            }
            progressLabelTwo.setText("");
            progressLabel.setText("");
            indexButton.setDisable(false);
        }
        else{
            textLabel.setText("No file is chosen.");
        }
    }

    public void indexJSONFiles(int fileNo) throws IOException {

        File file = new File(choosenJsonFiles.get(fileNo));

        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        count = 1;
        IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(indexDestinationFolders.get(fileNo))), iwc);

        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if(fileReader != null){
            totalLine = Files.lines(Paths.get(choosenJsonFiles.get(fileNo))).count();
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while (null != (line = bufferedReader.readLine())) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run() {
                        progressLabelTwo.setText("Current file: " + choosenJsonFiles.get(fileNo));
                        progressLabel.setText("Total documents to be indexed: " + totalLine + ". Now indexing " + count);
                    }
                });
                fieldValuePair = new HashMap<>();
                doc = new Document();
                JsonParser parser = new JsonParser();
                JsonObject jsonObject = parser.parse(line).getAsJsonObject();

                readerMethod(jsonObject, "");
                for (int i = 0; i < allPossibleFields.length; i++) {
                    if(!fieldValuePair.containsKey(allPossibleFields[i])){
                        doc.add(new TextField(allPossibleFields[i], "", Field.Store.YES));
                    }
                }
                indexWriter.addDocument(doc);
                count++;

            }
            indexWriter.close();
            try {
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else{
            System.out.println("fileReader is null");
        }
    }


    public void readerMethod(JsonObject jsonObject, String keyDepth){

        for (Map.Entry<String, JsonElement> e: jsonObject.entrySet()) {

            JsonElement jsonElement = jsonObject.get(e.getKey());

            if(jsonElement.isJsonPrimitive()){
                if(keyDepth.isEmpty()){
                    if(fieldValuePair.keySet().contains(e.getKey())){
                        fieldValuePair.put(e.getKey(), fieldValuePair.get(e.getKey()) + " ||| " + e.getValue().getAsString());
                        doc.removeField(e.getKey());
                        doc.add(new TextField(e.getKey(), fieldValuePair.get(e.getKey()) + " ||| " + e.getValue().getAsString(), Field.Store.YES));
                    }
                    else {
                        fieldValuePair.put(e.getKey(), e.getValue().getAsString());
                        doc.add(new TextField(e.getKey(), e.getValue().getAsString(), Field.Store.YES));
                    }
                }
                else {
                    if(fieldValuePair.keySet().contains(keyDepth + "-->" + e.getKey())){
                        fieldValuePair.put(keyDepth + "-->" + e.getKey(), fieldValuePair.get(keyDepth + "-->" + e.getKey()) + " ||| " + e.getValue().getAsString());
                        doc.removeField(keyDepth + "-->" + e.getKey());
                        doc.add(new TextField(keyDepth + "-->" + e.getKey(), fieldValuePair.get(keyDepth + "-->" + e.getKey()) + " ||| " + e.getValue().getAsString(), Field.Store.YES));
                    }
                    else {
                        fieldValuePair.put(keyDepth + "-->" + e.getKey(), e.getValue().getAsString());
                        doc.add(new TextField(keyDepth + "-->" + e.getKey(), e.getValue().getAsString(), Field.Store.YES));
                    }
                }
            }

            else{
                for (JsonElement je: jsonElement.getAsJsonArray()) {
                    if(je.isJsonObject()) {
                        readerMethod(je.getAsJsonObject(), e.getKey());
                    }
                    else if(!je.isJsonArray() && !je.isJsonObject()){
//                        System.out.println(e.getKey() + " " + je.getAsString());
                    }
                    else{
                        if(keyDepth.isEmpty()){
                            fieldValuePair.put(e.getKey(), "");
                            doc.add(new TextField(e.getKey(), "", Field.Store.YES));
                        }
                        else {
                            if(fieldValuePair.keySet().contains(keyDepth + "-->" + e.getKey())){
                                fieldValuePair.put(keyDepth + "-->" + e.getKey(), fieldValuePair.get(keyDepth + "-->" + e.getKey()) + " ||| , " + e.getValue().toString());
                                doc.removeField(keyDepth + "-->" + e.getKey());
                                doc.add(new TextField(keyDepth + "-->" + e.getKey(), fieldValuePair.get(keyDepth + "-->" + e.getKey()) + " ||| , " + e.getValue().toString(), Field.Store.YES));
                            }
                            else {
                                fieldValuePair.put(keyDepth + "-->" + e.getKey(), e.getValue().getAsString());
                                doc.add(new TextField(keyDepth + "-->" + e.getKey(), e.getValue().getAsString(), Field.Store.YES));
                            }
                        }
                    }
                }
            }

        }


    }

    public static void main(String[] args) {
        launch(args);
    }
}

