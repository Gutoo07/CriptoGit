package fateczl.TrabalhoLabEngSw.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import fateczl.TrabalhoLabEngSw.model.Arquivo;
import fateczl.TrabalhoLabEngSw.model.Commite;
import fateczl.TrabalhoLabEngSw.model.Diretorio;
import fateczl.TrabalhoLabEngSw.model.Repositorio;
import fateczl.TrabalhoLabEngSw.model.Usuario;
import fateczl.TrabalhoLabEngSw.persistence.RepositorioRepository;
import fateczl.TrabalhoLabEngSw.service.RepositorioService;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/repositorio")
public class RepositorioController {
	@Autowired
	private RepositorioRepository repRep;
	private RepositorioService repService;
	@Autowired
	private ArquivoController arqControl;
	@Autowired
	private CommitController comControl;
	@Autowired
	private DiretorioController dirControl;
	
	@GetMapping("/listagem")
	public ModelAndView carregaRepositorios(@CookieValue(name = "user_id",defaultValue = "") String user_id) {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("repositorio/listagem");
		
		// Carregar repositórios do banco de dados
		Usuario usuario = new Usuario();
		usuario.setId(Long.valueOf(user_id));
		List<Repositorio> lista = repRep.findByUsuario(usuario);
		
		// Passar dados para o frontend via Thymeleaf
		mv.addObject("repositories", lista);
		mv.addObject("repositoriesCount", lista.size());
		
		return mv;
	}
	@GetMapping("/criar")
	public ModelAndView criacaoRepositorio(@CookieValue(name = "user_id", defaultValue = "") String user_id) {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("repositorio/criar");
		mv.addObject("repositorio", new Repositorio());
		return mv;
	}
	
	@GetMapping("/acessar")
	public ModelAndView acessarRepositorios(@RequestParam(name = "rep_id", required = true) Long repId,
											@RequestParam(name = "commit_id", required = false) Long commitId,
	                                         @CookieValue(name = "user_id", defaultValue = "") String user_id) {
	    ModelAndView mv = new ModelAndView();
		System.out.println("Repositorio ID:"+repId);


	    Usuario usuario = new Usuario();
	    usuario.setId(Long.valueOf(user_id));

	    Optional<Repositorio> repositorioOpt = repRep.findById(repId);
	    List<Arquivo> arquivos;
	    Commite commit = new Commite();
	    if (repositorioOpt.isPresent()) {
	        System.out.println(repositorioOpt.get().getNome());
	        mv.addObject("repositorio", repositorioOpt.get());
	    } else {
	        mv.addObject("erro", "Repositório não encontrado");
	    }
	    
	    if (commitId == null) {
		    /*Achar os arquivos do ultimo commit desse repositorio*/
		     arquivos = arqControl.findLastByRepositorio(repId); 
		     commit = comControl.getLast(repId);
	    } else {
	    	 arquivos = arqControl.findByCommit(commitId, repId);
	    }

	    List<Commite> commits = comControl.getAllByRepId(repId);
	    if (commits.isEmpty()) {
	    	commits = null;
	    }
	    
	    mv.addObject("arquivos", arquivos);
	    mv.addObject("commits", commits);
	    mv.addObject("commit", commit);
	    mv.setViewName("repositorio/acessar");
	    return mv;
	}
	@GetMapping("/excluirRepositorio")
	public ModelAndView excluirRepositorio(@RequestParam Map<String, String> params, @CookieValue(name = "user_id", defaultValue = "") String user_id) {
		String rep_id = params.get("rep_id");
		Optional<Repositorio> repositorioOpt = repRep.findById(Long.valueOf(rep_id));
		if (repositorioOpt.isPresent()) {
			if (repositorioOpt.get().getUsuario().getId() == Long.valueOf(user_id)) {
				List<Commite> commits = comControl.getAllByRepIdOrderByIdDesc(repositorioOpt.get());
				for (Commite c : commits) {
					List<Diretorio> diretorios = dirControl.findAllByCommit(c);
					for (Diretorio d : diretorios) {
						List<Arquivo> arquivos = arqControl.findAllByDiretorio(d);
						for (Arquivo a : arquivos) {
							System.err.println();
							System.err.println("Arquivo "+a.getId());
							arqControl.excluir(a);
						}
						System.err.println("Diretorio "+d.getId());
						dirControl.excluir(d);
					}
					System.err.println("Commit "+c.getId());
					comControl.excluir(c);
				}
 				repRep.delete(repositorioOpt.get());
			}
		}
		ModelAndView mv = new ModelAndView();
		//Usuario usuario = new Usuario();
		//usuario.setId(Long.valueOf(user_id));
		//List<Repositorio> lista = repRep.findByUsuario(usuario);
		//mv.setViewName("repositorio/listagem");		
		mv.setViewName("redirect:listagem");
		//mv.addObject("listaRep", lista);
		return mv;
	}
	
	@PostMapping("/criarRepositorio")
	public ModelAndView criarRepositorio(Repositorio repositorio,
			@CookieValue(value = "user_id", defaultValue = "") String user_id) throws Exception {
		ModelAndView mv = new ModelAndView();
		
		Usuario usuario = new Usuario();
		usuario.setId(Long.parseLong(user_id));
		repService = new RepositorioService();
		repositorio.setChaveSimetrica(repService.gerarChaveSimetrica().getEncoded());
		repositorio.setUsuario(usuario);
		repRep.save(repositorio);
		//mv.setViewName("redirect:repositorios");
		mv.setViewName("redirect:listagem");
		//mv.setViewName("repositorio/listagem");
		//return carregaRepositorios(user_id);
		return mv;
	}
	/**
	 * Baixa os arquivos de um determinado commit
	 * @param params
	 * @param user_id
	 * @return
	 * @throws IOException
	 */
	@PostMapping("/baixarCommit")
	public ModelAndView baixarCommit(@RequestParam Map<String, String> params,
			@CookieValue(name = "user_id", defaultValue = "") String user_id) throws IOException {
	    Long commitId = Long.valueOf(params.get("commit_id"));
	    Long repId = Long.valueOf(params.get("rep_id"));
    	List<Arquivo> arquivos = arqControl.findByCommit(commitId, repId);
    	for (Arquivo a : arquivos) {
    		arqControl.download(a);
    	}
		ModelAndView mv = new ModelAndView();
		mv.setViewName("login/login");
		mv.addObject("usuario", new Usuario());
		return mv;
	}

	/**
	 * Baixa a versão mais recente de todos os arquivos do repositorio
	 * @param params
	 * @param user_id
	 * @return
	 * @throws IOException
	 */
	@PostMapping("/baixarRepositorio") 
	public ModelAndView baixarRepositorio(@RequestParam Map<String, String> params,
			@CookieValue(name = "user_id", defaultValue = "") String user_id) throws IOException {
		Long repId = Long.valueOf(params.get("rep_id"));
		List<Arquivo> arquivos = arqControl.findLastByRepositorio(repId);
		for (Arquivo a : arquivos) {
			arqControl.download(a);
		}
		ModelAndView mv = new ModelAndView();
		mv.setViewName("login/login");
		mv.addObject("usuario", new Usuario());
		return mv;
	}
}
