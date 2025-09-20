package fateczl.TrabalhoLabEngSw.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

import fateczl.TrabalhoLabEngSw.model.Arquivo;
import fateczl.TrabalhoLabEngSw.persistence.ArquivoRepository;
@Service
public class ArquivoService {
    @Autowired
    private ArquivoRepository arqRep;

    public List<Arquivo> findByCommit(Long commitId, Long repId) {
        return arqRep.findByCommiteAndRepositorio(commitId, repId);
    }
    
    /**
     * Busca a versão mais recente de cada arquivo único em um repositório.
     * Cada arquivo retornado representa a versão mais recente baseada no commit.
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
}
