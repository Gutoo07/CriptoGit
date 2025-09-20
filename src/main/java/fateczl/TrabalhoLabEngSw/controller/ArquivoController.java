package fateczl.TrabalhoLabEngSw.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fateczl.TrabalhoLabEngSw.model.Arquivo;
import fateczl.TrabalhoLabEngSw.model.Blob;
import fateczl.TrabalhoLabEngSw.model.Commite;
import fateczl.TrabalhoLabEngSw.model.Diretorio;
import fateczl.TrabalhoLabEngSw.model.Repositorio;
import fateczl.TrabalhoLabEngSw.model.Usuario;
import fateczl.TrabalhoLabEngSw.persistence.ArquivoRepository;
import fateczl.TrabalhoLabEngSw.persistence.BlobRepository;
import fateczl.TrabalhoLabEngSw.persistence.CommitRepository;
import fateczl.TrabalhoLabEngSw.persistence.DiretorioRepository;
import fateczl.TrabalhoLabEngSw.persistence.RepositorioRepository;
import fateczl.TrabalhoLabEngSw.persistence.UsuarioRepository;
import fateczl.TrabalhoLabEngSw.service.EncryptionService;

@Controller
public class ArquivoController {
	@Autowired
	private ArquivoRepository arqRep;
	@Autowired
	private BlobRepository blobRep;
	@Autowired
	private UsuarioRepository userRep;
	@Autowired
	private CommitRepository commitRep;
	@Autowired
	private DiretorioRepository dirRep;
	@Autowired
	private RepositorioRepository repRep;
	@Autowired
	private EncryptionService encryptionService;
	

	@PostMapping("/arquivo/uploadArquivo")
	public ModelAndView upload(@RequestParam("arquivo") MultipartFile[] arquivos, @RequestParam("msg") String msg,
			@CookieValue(name = "user_id", defaultValue = "") String user_id,
			@RequestParam(name = "rep_id", required = true) Long repId) throws IOException, NoSuchAlgorithmException {


        Optional<Usuario> autorOpt = userRep.findById(Long.valueOf(user_id));
        Usuario autor = autorOpt.get();

        //Optional<Repositorio> repositorio = repRep.findById(rep_Id);
        Optional<Repositorio> repositorioOpt = repRep.findById(repId);
        Repositorio repositorio = repositorioOpt.get();
        
        Commite commitAnterior = commitRep.findLastCommitByRepositorio(repId);
        Commite commit = new Commite();
        commit.setAutor(autor);
        commit.setMsg(msg);
        commit.setOrigem(repositorio);
		//Buscar o commit anterior para criar a sequencia temporal de versionamento        

        if (commitAnterior != null) {
        	commit.setAnterior(commitAnterior);
        }
        commitRep.save(commit);
        
		Diretorio diretorio = new Diretorio();
		diretorio.setNome("Tree node");		
		diretorio.setCommit(commit);
		dirRep.save(diretorio);
        
        
	    for (MultipartFile arquivo : arquivos) {
	        Blob blob = new Blob();
	        byte[] bytes = arquivo.getBytes();
	        
	        /*
			// Criptografar o conteúdo do arquivo
	        try {
	            byte[] encryptedContent = encryptionService.encrypt(bytes);
	            blob.setConteudo(encryptedContent);
	        } catch (Exception e) {
	            throw new RuntimeException("Erro ao criptografar arquivo: " + e.getMessage(), e);
	        }

	        // Calcular SHA1 do conteúdo original (antes da criptografia)
			*/
	        MessageDigest md = MessageDigest.getInstance("SHA1");
	        byte[] sha1bytes = md.digest(bytes);
	        BigInteger bigInt = new BigInteger(1, sha1bytes);
	        String sha1 = bigInt.toString(16);
	        blob.setSha1(sha1);
	        
	        Arquivo novo = new Arquivo();
	        /*Testar se o arquivo upado ja existe ou foi alterado, para salvar ele ou nao*/
	        if (blobRep.findBySha1(sha1) == null) {
	        	/*Arquivo novo/diferente: salva no bd*/
		        blobRep.save(blob);	        
		        
		        novo.setNome(arquivo.getOriginalFilename());
		        novo.setBlob(blob);
		        novo.setDiretorioPai(diretorio);
	        } else {
	        	/*Arquivo existente: salva apenas a referencia do ultimo*/
	        	Blob antigo = blobRep.findBySha1(sha1);
	        	/*Pega o nome do arquivo antigo*/
		        novo.setNome(arquivo.getOriginalFilename());
		        novo.setBlob(antigo);
	        	novo.setDiretorioPai(diretorio);
	        }   
	        arqRep.save(novo);
	    }	 
	    return index(autor);
	}
	
	@PostMapping("/arquivo/uploadToServer")
	@ResponseBody
	public ResponseEntity<String> uploadToServer(
			@RequestParam("arquivo") MultipartFile[] arquivos, 
			@RequestParam("msg") String msg,
			@CookieValue(name = "user_id", defaultValue = "") String user_id,
			@RequestParam(name = "rep_id", required = true) Long repId,
			@RequestParam(name = "server_url", required = true) String serverUrl) {
		
		try {
			// Validar se arquivos foram enviados
			if (arquivos == null || arquivos.length == 0) {
				return ResponseEntity.badRequest().body("Nenhum arquivo foi enviado.");
			}
			
			// Validar se a mensagem foi preenchida
			if (msg == null || msg.trim().isEmpty()) {
				return ResponseEntity.badRequest().body("Mensagem do commit é obrigatória.");
			}
			
			// Validar URL do servidor
			if (serverUrl == null || serverUrl.trim().isEmpty()) {
				return ResponseEntity.badRequest().body("URL do servidor é obrigatória.");
			}
			
			// Processar cada arquivo
			for (MultipartFile arquivo : arquivos) {
				// 1. Criptografar o arquivo
				byte[] arquivoOriginal = arquivo.getBytes();
				byte[] arquivoCriptografado = encryptionService.encrypt(arquivoOriginal);
				
				// 2. Calcular hash SHA1 do arquivo original
				MessageDigest md = MessageDigest.getInstance("SHA1");
				byte[] sha1bytes = md.digest(arquivoOriginal);
				BigInteger bigInt = new BigInteger(1, sha1bytes);
				String sha1 = bigInt.toString(16);
				
				// 3. Enviar arquivo criptografado para o servidor externo
				boolean sucesso = enviarArquivoParaServidor(
					arquivoCriptografado, 
					arquivo.getOriginalFilename(), 
					sha1, 
					msg, 
					serverUrl
				);
				
				if (!sucesso) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body("Erro ao enviar arquivo: " + arquivo.getOriginalFilename());
				}
			}
			
			return ResponseEntity.ok("Arquivos enviados com sucesso para o servidor!");
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Erro interno: " + e.getMessage());
		}
	}
	
	/**
	 * Envia um arquivo criptografado para um servidor externo
	 */
	private boolean enviarArquivoParaServidor(byte[] arquivoCriptografado, String nomeArquivo, 
			String sha1, String mensagemCommit, String serverUrl) {
		
		try {
			// Criar cliente HTTP
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(30))
				.build();
			
			// Preparar dados para envio
			StringBuilder jsonData = new StringBuilder();
			jsonData.append("{");
			jsonData.append("\"nomeArquivo\":\"").append(nomeArquivo).append("\",");
			jsonData.append("\"sha1\":\"").append(sha1).append("\",");
			jsonData.append("\"mensagemCommit\":\"").append(mensagemCommit).append("\",");
			jsonData.append("\"conteudoCriptografado\":\"").append(java.util.Base64.getEncoder().encodeToString(arquivoCriptografado)).append("\"");
			jsonData.append("}");
			
			// Criar requisição HTTP POST
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(serverUrl))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonData.toString()))
				.timeout(Duration.ofSeconds(60))
				.build();
			
			// Enviar requisição
			HttpResponse<String> response = client.send(request, 
				HttpResponse.BodyHandlers.ofString());
			
			// Verificar se foi bem-sucedido
			return response.statusCode() >= 200 && response.statusCode() < 300;
			
		} catch (Exception e) {
			System.err.println("Erro ao enviar arquivo para servidor: " + e.getMessage());
			return false;
		}
	}	

	@GetMapping("/upload")
 	public ModelAndView carregaPaginaUpload(@RequestParam(name = "rep_id", required = true) Long repId) {
		ModelAndView mv = new ModelAndView();
		System.out.println("Repositorio ID:"+repId);
		
		Optional<Repositorio> repositorioOpt = repRep.findById(repId);
	    
	    if (repositorioOpt.isPresent()) {
	        System.out.println("Repositorio de ID:"+repositorioOpt.get().getId()+" "+repositorioOpt.get().getNome());
	        mv.addObject("repositorio", repositorioOpt.get());
	    } else {
	        mv.addObject("erro", "Repositório não encontrado");
	    }

		mv.setViewName("arquivo/upload");
 		return mv;
 	}
	
	public List<Arquivo> findLastByRepositorio(Long repId) {
		Commite ultimoCommit = commitRep.findLastCommitByRepositorio(repId);
		if (ultimoCommit != null) {
			return arqRep.findByCommiteAndRepositorio(ultimoCommit.getId(), repId);
		} else {
			return null;
		}
	}
	
	/**
	 * Busca a versão mais recente de cada arquivo único em um repositório.
	 * Diferente do findLastByRepositorio, este método encontra a versão mais recente
	 * de cada arquivo individualmente, não apenas os arquivos do último commit.
	 */
	public List<Arquivo> findLatestVersionsByRepositorio(Long repId) {
		return arqRep.findLatestVersionsByRepositorio(repId);
	}
	
	/**
	 * Busca a versão mais recente de um arquivo específico em um repositório.
	 */
	public Arquivo findLatestVersionByNomeAndRepositorio(Long repId, String nomeArquivo) {
		return arqRep.findLatestVersionByNomeAndRepositorio(repId, nomeArquivo);
	}
	public List<Arquivo> findByCommit(Long commitId, Long repId) {
		return arqRep.findByCommiteAndRepositorio(commitId, repId);
	}
	public List<Arquivo> findAllByDiretorio(Diretorio diretorio) {
		return arqRep.findByDiretorioPai(diretorio);
	}
	public ModelAndView index(Usuario usuario) {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("home/index");
		mv.addObject("usuario", usuario);
		return mv;
	}
	public void download(Arquivo arquivo) throws IOException {
		Blob blob = blobRep.findById(arquivo.getBlob().getId());
		Path currentDirectoryPath = FileSystems.getDefault().getPath("");
		String currentDirectoryName = currentDirectoryPath.toAbsolutePath().toString();
		String downloadPath = currentDirectoryName+"/downloads";
		
		String arquivoNome = arquivo.getNome();
		String[] caminhos = arquivoNome.split("/");
		File file;
		if (caminhos.length > 1) {
			int i;
			for (i = 0; i < (caminhos.length - 1); i++) {
				downloadPath = downloadPath + "/" + caminhos[i];
			}
			new File(downloadPath).mkdirs();
			file = new File(downloadPath+"/"+caminhos[i]);
		} else {
			new File(downloadPath).mkdirs();
			file = new File(downloadPath+"/"+arquivoNome);
		}

		try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			/*
			// Descriptografar o conteúdo antes de salvar
			try {
				byte[] decryptedContent = encryptionService.decrypt(blob.getConteudo());
				fileOutputStream.write(decryptedContent);
			} catch (Exception e) {
				throw new RuntimeException("Erro ao descriptografar arquivo: " + e.getMessage(), e);
			}
			*/
			fileOutputStream.write(blob.getConteudo());
			fileOutputStream.close();
		}
	}
	public void excluir(Arquivo arquivo) {
		arqRep.delete(arquivo);
	}

}
//