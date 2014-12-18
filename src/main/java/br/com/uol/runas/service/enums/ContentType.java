/*
 *    Copyright 2013-2014 UOL - Universo Online Team
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package br.com.uol.runas.service.enums;

import org.springframework.http.MediaType;

public enum ContentType {
	
	JSON("json", MediaType.APPLICATION_JSON),
	HTML("html", MediaType.TEXT_HTML);
	
	private String extension;
	private MediaType contentType;

	private ContentType(String extension, MediaType contentType) {
		this.extension = extension;
		this.contentType = contentType;
	}

	public String getExtension() {
		return extension;
	}

	public MediaType getContentType() {
		return contentType;
	}
}
