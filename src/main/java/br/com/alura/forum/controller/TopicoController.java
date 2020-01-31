package br.com.alura.forum.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import br.com.alura.forum.controller.dto.DetalhesDoTopicoDto;
import br.com.alura.forum.controller.dto.TopicoDto;
import br.com.alura.forum.controller.form.AtualizarTopicoForm;
import br.com.alura.forum.controller.form.TopicoForm;
import br.com.alura.forum.modelo.Topico;
import br.com.alura.forum.repository.CursoRepository;
import br.com.alura.forum.repository.TopicoRepository;

//Para substituir o ResponseBody que serve para que o Spring nao faça uma navegação para uma pagina
//porem queremos devolver o que estiver retornando no metodo, entao ele pega o que esta no corpo da requisiçaõ
@RestController 

//Mapear o endereço
@RequestMapping("/topicos")
public class TopicoController {
	
	
	@Autowired //injeção de dependencia
	private TopicoRepository topicoRepository;
	
	@Autowired
	private CursoRepository cursoRepository;

//	@GetMapping
//	@Cacheable(value = "listaDeTopicos")
//								//para mostrar pro Spring que é um parametro de request/url, entao esse parametro é obrigatorio
//	public Page<TopicoDto> lista(@RequestParam(required=false) String filtro,
//			@PageableDefault(sort = "id", direction = Direction.DESC, page = 0, size = 10) Pageable paginacao){
//		
//		if(filtro == null) {
//			Page<Topico> topicos = topicoRepository.findAll(paginacao);
//			return TopicoDto.converter(topicos);
//		}else {
//			Page<Topico> topicos = topicoRepository.findByCursoNome(filtro, paginacao);
//			return TopicoDto.converter(topicos);
//		}
//	}
	
	//ResponseEntity retorno de entidade, ou seja, retornar por exemplo o 201, 404, retornar URI
	//uriBilder serve para que ele traga o caminho base ja pra uir e a gente só adiciona os caminhos especificos
	
	@PostMapping
	@Transactional
	@CacheEvict(value = "listaDeTopicos", allEntries = true)
	public ResponseEntity<TopicoDto> cadastrar(@RequestBody @Valid TopicoForm form, UriComponentsBuilder uriBuilder) {
		Topico topico = form.converter(cursoRepository);
		topicoRepository.save(topico);
		
		URI uri = uriBuilder.path("/topico/{id}").buildAndExpand(topico.getId()).toUri();
		
		return ResponseEntity.created(uri).body(new TopicoDto(topico));
	}
	
	@GetMapping("/{id}")
	@Cacheable(value = "listaDeTopicos")
	public ResponseEntity<DetalhesDoTopicoDto> detalhar(@PathVariable Long id) {
		//opitional serve para trazer alguns metodos a mais para o objeto como o findById e pode ser que tenha um resgistro ou nao tenha (isPresent)
		Optional<Topico> topico = topicoRepository.findById(id); //buscar por ID
		if (topico.isPresent()) {
														//topico.get() serve para carrega ro topico que esta dentro do optional
			return ResponseEntity.ok(new DetalhesDoTopicoDto(topico.get())) ;
		}
		
		return ResponseEntity.notFound().build();
		}
	
	@PutMapping("/{id}")
	@Transactional
	@CacheEvict(value = "listaDeTopicos", allEntries = true)
	public ResponseEntity<TopicoDto> atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarTopicoForm form){
		Optional<Topico> optional = topicoRepository.findById(id); //buscar por ID
		if (optional.isPresent()) {
			Topico topico = form.atualizar(id, topicoRepository);
			return ResponseEntity.ok(new TopicoDto(topico));
		}
		
		return ResponseEntity.notFound().build();
		
	}
	
	@DeleteMapping("/{id}")
	@Transactional
	@CacheEvict(value = "listaDeTopicos", allEntries = true)
	public ResponseEntity<?> remover(@PathVariable Long id){
		Optional<Topico> optional = topicoRepository.findById(id); //buscar por ID
		if (optional.isPresent()) {
			topicoRepository.deleteById(id);
			return ResponseEntity.ok().build();
		}
		
		return ResponseEntity.notFound().build();
		
	}
	
	
	@GetMapping
	public byte[] criarCsv() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		
        List<TopicoDto> pessoas = new ArrayList<>();
		List<Topico> topicos = topicoRepository.findAll();
		
		pessoas = TopicoDto.converter(topicos.stream().collect(Collectors.toList()));

        Writer writer = Files.newBufferedWriter(Paths.get("pessoas.csv"));
        StatefulBeanToCsv<TopicoDto> beanToCsv = new StatefulBeanToCsvBuilder<TopicoDto>(writer).build();
        
        beanToCsv.write(pessoas);
        
        writer.flush();
        writer.close();
        
        return pegarArquivo("pessoas.csv");
	}
	
	private byte[] pegarArquivo(String nomeDoArquivo) throws IOException  {
		File arquivo = new File(nomeDoArquivo);
		
		byte[] bytes = loadFile(arquivo);
	    byte[] encoded = Base64.getEncoder().encode(bytes);
//	    String encodedString = new String(encoded);
	    
	    arquivo.delete();
	 
	    return encoded;
	}
	 
	   private byte[] loadFile(File file) throws IOException {
	       byte[] bytes;
	       try (InputStream is = new FileInputStream(file)) {
	           long length = file.length();
	           if (length > Integer.MAX_VALUE) {
	               throw new IOException("File to large " + file.getName());
	           }
	           bytes = new byte[(int) length];
	           int offset = 0;
	           int numRead = 0;
	           while (offset < bytes.length
	                   && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
	               offset += numRead;
	           }
	           if (offset < bytes.length) {
	               throw new IOException("Could not completely read file " + file.getName());
	           }
	       }
	       return bytes;
	   }
	
}
