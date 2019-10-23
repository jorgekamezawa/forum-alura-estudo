package br.com.alura.forum.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.alura.forum.controller.form.AutenticacaoForm;
import br.com.alura.forum.controller.form.TopicoForm;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {
	
	@PostMapping
	public ResponseEntity<?> autenticar(@RequestBody @Valid AutenticacaoForm form){
		System.out.println(form.getEmail());
		System.out.println(form.getSenha());
		return ResponseEntity.ok().build();
		
	}

}
