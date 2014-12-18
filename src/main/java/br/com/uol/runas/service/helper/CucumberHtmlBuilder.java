package br.com.uol.runas.service.helper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.Script;
import com.hp.gagawa.java.elements.Style;

public class CucumberHtmlBuilder {

	public static String build(Map<Class<?>, String> logMap) throws IOException{

		Html html = null;
		int i = 0;
		for(String log : logMap.values()){
			if (i == 0){
				html = makeHtml(Paths.get(log));
			}else{
				html.appendChild(appendJs(new String( Files.readAllBytes(Paths.get(log + "/report.js")) )));
			}
			i++;
		}
		return html.write();
	}

	private static Html makeHtml(Path dir) throws IOException{
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			final Html html = new Html();
			String content;
			for(Path path : stream){

				content = new String(Files.readAllBytes(path));
				
				if(path.getFileName().toString().endsWith("js")){
					if(path.getFileName().toString().contains("jquery")){
						html.appendChild(0, appendJs(content));
					}else{
						html.appendChild(appendJs(content));
					}
				}
				
				if(path.getFileName().toString().endsWith("css")){
					html.appendChild(appendCss(content));
				}
				
				if(path.getFileName().toString().endsWith("html")){
					html.appendChild(appendDiv(content));
				}
			}
			return html;
		}
	}
	
	private static Div appendDiv(String content) {
		final String divText = content.substring(content.indexOf("<div"), content.lastIndexOf("</div>")) + "</div>";
		Div div = new Div() {
			@Override
			public String write() {
				return divText;
			}
		};
		return div;
	}

	private static Script appendJs(String content) {
		return new Script("text/javascript").appendText(content);
	}
	
	private static Style appendCss(String content) {
		return new Style("text/css").appendText(content);
	}

}
