package br.com.uol.runas.concurrent;

import gherkin.deps.com.google.gson.Gson;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.Suite;

import br.com.uol.runas.service.enums.ContentType;
import br.com.uol.runas.service.helper.CucumberHtmlBuilder;
import br.com.uol.runas.service.helper.CucumberOptionsHelper;
import br.com.uol.runas.service.response.JUnitServiceResponse;
import cucumber.api.CucumberOptions;

@SuppressWarnings("unchecked")
public class JUnitTask implements Callable<JUnitServiceResponse> {

    private final String LOG_PATH_WITHOUT_EXTENSION = "/tmp/runas/";
    private final Set<Class<?>> classesToChange = new HashSet<>();
    private final Set<ContentType> foundTypes = new HashSet<>();
    private final Map<Class<?>, String> logMap = new HashMap<Class<?>, String>();
    private ContentType contentType;

    private final String[] suites;

    public JUnitTask(String[] suites) {
        this.suites = suites;
    }

    @Override
    public JUnitServiceResponse call() throws Exception {
        final Class<?>[] classes = new Class[suites.length];

        resetLists();

        for (int i = 0; i < suites.length; i++) {
            classes[i] = Thread.currentThread().getContextClassLoader().loadClass(suites[i]);
            prepareClass(classes[i]);
        }

        executeAlterClasses();

        final JUnitServiceResponse response = parse(JUnitCore.runClasses(classes));

        deleteLogs();

        return response;
    }

    //    @Override
    //    public JUnitServiceResponse call() throws Exception {
    //        final Class<?>[] classes = new Class[suites.length];
    //
    //
    //        for (int i = 0; i < suites.length; i++) {
    //            classes[i] = Thread.currentThread().getContextClassLoader().loadClass(suites[i]);
    //            System.out.println(classes[i]);
    //        }
    //
    //        return new JUnitServiceResponse(MediaType.APPLICATION_ATOM_XML, LOG_PATH_WITHOUT_EXTENSION, new JUnitCore().run(classes));
    //    }

    private JUnitServiceResponse parse(Result result) throws Exception {
        switch (contentType) {
        case HTML:
            return parseHtml(result);
        default:
            return parseJson(result);
        }
    }

    private JUnitServiceResponse parseHtml(Result result) throws Exception{
        return new JUnitServiceResponse(contentType.getContentType(), CucumberHtmlBuilder.build(logMap) , result);
    }

    private JUnitServiceResponse parseJson(Result result) throws Exception{

        final List<Object> logs = new ArrayList<>();
        for(String log : logMap.values()){
            logs.addAll(new Gson().fromJson(new String(Files.readAllBytes(Paths.get(log))), List.class));
        }

        return new JUnitServiceResponse(contentType.getContentType(), new Gson().toJson(logs) , result);
    }

    private void deleteLogs() throws Exception {
        for(String log : logMap.values()){
            deleteLogs(Paths.get(log));
        }
    }

    private void deleteLogs(Path path) throws IOException{
        if(Files.isDirectory(path)){
            FileUtils.deleteDirectory(path.toFile());
        }else{
            Files.deleteIfExists(path);
        }
    }

    private void executeAlterClasses() throws Exception {
        contentType = chooseContentType();
        setLogPath();
        alterClasses();
    }

    private void resetLists() {
        classesToChange.clear();
        foundTypes.clear();
        logMap.clear();
    }

    private void prepareClass(Class<?> clazz) throws Exception{

        if(clazz.isAnnotationPresent(Suite.SuiteClasses.class)){
            prepareSuiteClasses(clazz);
        }

        if(clazz.isAnnotationPresent(CucumberOptions.class)){
            prepareCucumberClasses(clazz);
        }
    }

    private void prepareSuiteClasses(Class<?> clazz) throws Exception {
        final Suite.SuiteClasses suiteClasses = clazz.getAnnotation(Suite.SuiteClasses.class);

        for(Class<?> c : suiteClasses.value()){

            if(c.isAnnotationPresent(CucumberOptions.class)){
                prepareCucumberClasses(c);
            }
            prepareClass(c);
        }
    }

    private void prepareCucumberClasses(Class<?> clazz) {
        CucumberOptions cucumberOptions = clazz.getAnnotation(CucumberOptions.class);
        if(cucumberOptions.format() != null){
            classesToChange.add(clazz);
            appendFormats(cucumberOptions);
        }
    }

    private void alterClasses() throws Exception{
        Annotation newCucumberOptions;
        for(Class<?> clazz : classesToChange){
            newCucumberOptions = changeCucumberOptions(clazz, clazz.getAnnotation(CucumberOptions.class));
            overrideCucumberOptions(newCucumberOptions, clazz);
        }
    }

    private void overrideCucumberOptions(Annotation annotation, Class<?> clazz) throws Exception{
        final Field field = Class.class.getDeclaredField("annotations");
        field.setAccessible(true);
        final Map<Class<? extends Annotation>, Annotation> annotations = (Map<Class<? extends Annotation>, Annotation>) field.get(clazz);
        annotations.put(CucumberOptions.class, annotation);
    }

    private void appendFormats(CucumberOptions cucumberOptions){
        final String[] oldFormats = cucumberOptions.format();
        String[] extensions;

        if(oldFormats != null){

            for(int i = 0; i < oldFormats.length; i++){
                extensions = oldFormats[i].split(":");
                switch (extensions[0]) {
                case "json":
                    foundTypes.add(ContentType.JSON);
                    break;
                case "html":
                    foundTypes.add(ContentType.HTML);
                    break;
                default:
                    break;
                }
            }
        }
    }

    private Annotation changeCucumberOptions(Class<?>clazz, CucumberOptions cucumberOptions){
        return CucumberOptionsHelper.newCucumberOptions(cucumberOptions, new String[]{contentType.getExtension() + ":" + logMap.get(clazz)});
    }

    private ContentType chooseContentType(){
        if(foundTypes.contains(ContentType.JSON)){
            return ContentType.JSON;
        }

        if(foundTypes.contains(ContentType.HTML)){
            return ContentType.HTML;
        }

        return ContentType.JSON;
    }

    private void setLogPath(){
        for(Class<?> clazz : classesToChange){
            logMap.put(clazz, LOG_PATH_WITHOUT_EXTENSION + System.currentTimeMillis() + "." + clazz.getCanonicalName() + "." + contentType.getExtension());
        }
    }
}
