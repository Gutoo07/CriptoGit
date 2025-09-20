package fateczl.TrabalhoLabEngSw.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import fateczl.TrabalhoLabEngSw.model.Arquivo;
import fateczl.TrabalhoLabEngSw.model.Commite;
import fateczl.TrabalhoLabEngSw.service.ArquivoService;
import fateczl.TrabalhoLabEngSw.service.CommitService;

@RestController
@RequestMapping("/api")
public class ApiController {    
    @Autowired
    private ArquivoService arqService;
    @Autowired
    private CommitService comService;
    
    // Requisição para buscar os arquivos do commit selecionado
    @GetMapping("/get_commit")
    public ResponseEntity<List<Arquivo>> getCommit(Long rep_id, Long commit_id) {
        try {
            List<Arquivo> arquivos = arqService.findByCommit(commit_id, rep_id);
            return ResponseEntity.ok(arquivos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // Requisição para buscar as informações do commit selecionado
    @GetMapping("/get_commit_info")
    public ResponseEntity<Commite> getCommitInfo(Long commit_id) {
        try {
            Optional<Commite> commit = comService.findById(commit_id);
            if (!commit.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(commit.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /**
	 * API REST para buscar as versões mais recentes de todos os arquivos de um repositório
	 * Retorna apenas o status da requisição (eventualmente será chamada para API externa)
	 */
	@GetMapping("/repositorio/{repId}/latest-files")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getLatestFilesFromRepository(@PathVariable Long repId) {
		try {            
            // Implementação atual (temporária)
			List<Arquivo> arquivos = arqService.findLatestVersionsByRepositorio(repId);
			
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Arquivos recuperados com sucesso (implementação local)");
			response.put("count", arquivos != null ? arquivos.size() : 0);
			response.put("repositorioId", repId);
			response.put("implementation", "local");

            // TODO: Implementação futura com chamada para API externa usando HttpClient
            
            String externalApiUrl = "http://localhost:8083/webhook/";
            
            try {
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(externalApiUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
                
                HttpResponse<String> externalResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (externalResponse.statusCode() >= 200 && externalResponse.statusCode() < 300) {
                    Map<String, Object> externalResponseMap = new HashMap<>();
                    externalResponseMap.put("status", "success");
                    externalResponseMap.put("message", "Arquivos recuperados com sucesso da API externa");
                    externalResponseMap.put("externalApiStatus", externalResponse.statusCode());
                    externalResponseMap.put("externalApiResponse", externalResponse.body());
                    externalResponseMap.put("repositorioId", repId);
                    externalResponseMap.put("method", "HttpClient");
                    return ResponseEntity.ok(externalResponseMap);
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Erro na API externa: " + externalResponse.statusCode());
                    errorResponse.put("repositorioId", repId);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
                }
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Erro de conexão com API externa: " + e.getMessage());
                errorResponse.put("repositorioId", repId);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
            }
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Erro ao recuperar arquivos: " + e.getMessage());
			errorResponse.put("repositorioId", repId);
			errorResponse.put("implementation", "local");
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}
}
