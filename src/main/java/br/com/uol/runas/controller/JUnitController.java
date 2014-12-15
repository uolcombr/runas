package br.com.uol.runas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.uol.runas.service.JUnitService;

@RestController
public class JUnitController {
	
	@Autowired
	private JUnitService jUnitService;
	
	@RequestMapping("/junit")
	public String run(@RequestParam("path") String path, @RequestParam("suits") String[] suits) throws Exception {
		
		jUnitService.scan(path, suits);
		
		return "Oi";		
	} 
	
	
}
