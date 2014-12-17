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
package br.com.uol.runas.controller;

import static org.apache.commons.lang3.StringUtils.substringBetween;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.uol.runas.service.JUnitService;

@RestController
public class JUnitController {

    @Autowired
    private JUnitService jUnitService;

    @RequestMapping("/junit/**/{pack:(?:.+\\.[jwe]ar)}/{suites:(?:.+\\.)+[_A-Z].+}")
    public String runPackage(HttpServletRequest request,
            @PathVariable("pack") String pack,
            @PathVariable("suites") String[] suites,
            @RequestParam(required=false, value="retries") Integer retries,
            @RequestParam(required=false, value="interval") Integer interval) throws Exception {

        final String path = substringBetween(request.getRequestURI(), "/junit", "/" + suites[0]);

        return String.valueOf(jUnitService.runTests(path, suites).wasSuccessful());
    }

    @RequestMapping("/junit/**/{pack:(?:.*(?!\\.[jwe]ar))}/{suites:(?:.+\\.)+[_A-Z].+}")
    public String runDir(HttpServletRequest request,
            @PathVariable("suites") String[] suites,
            @RequestParam(required=false, value="retries") Integer retries,
            @RequestParam(required=false, value="interval") Integer interval) throws Exception {

        final String path = substringBetween(request.getRequestURI(), "/junit", "/" + suites[0]);

        return String.valueOf(jUnitService.runTests(path, suites).wasSuccessful());
    }
}
