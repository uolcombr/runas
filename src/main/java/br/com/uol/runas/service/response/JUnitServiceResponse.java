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
package br.com.uol.runas.service.response;

import org.junit.runner.Result;
import org.springframework.http.MediaType;

public class JUnitServiceResponse {
	
	private MediaType mediaType;
	private String log;
	private Result result;

	public JUnitServiceResponse(MediaType mediaType, String log, Result result) {
		this.mediaType = mediaType;
		this.log = log;
		this.result = result;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	public String getLog() {
		return log;
	} 
	
	public void setLog(String log) {
		this.log = log;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}
}
