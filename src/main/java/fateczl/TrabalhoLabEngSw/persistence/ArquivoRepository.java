package fateczl.TrabalhoLabEngSw.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fateczl.TrabalhoLabEngSw.model.Arquivo;
import fateczl.TrabalhoLabEngSw.model.Blob;
import fateczl.TrabalhoLabEngSw.model.Diretorio;

public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {

	@Query(nativeQuery = true, value = "select a.id, a.nome, a.blob_id, a.diretorio_pai_id from arquivo a\r\n"
			+ "\r\n"
			+ "inner join diretorio d\r\n"
			+ "on a.diretorio_pai_id = d.id\r\n"
			+ "inner join commite c\r\n"
			+ "on d.commite_id = c.id\r\n"
			+ "inner join repositorio r\r\n"
			+ "on c.repositorio_origem_id = r.id\r\n"
			+ "where c.id = :commiteId and r.id=:repositorioId")
	public List<Arquivo> findByCommiteAndRepositorio(Long commiteId, Long repositorioId);
	
	public Arquivo findByBlob(Blob blob);
	
	public List<Arquivo> findByDiretorioPai(Diretorio diretorioPai);
	
	/**
	 * Busca a versão mais recente de cada arquivo único em um repositório.
	 * Para cada nome de arquivo, retorna apenas a versão do commit mais recente.
	 */
	@Query(nativeQuery = true, value = 
		"WITH LatestFiles AS ( " +
		"    SELECT a.nome, MAX(c.id) as latest_commit_id " +
		"    FROM arquivo a " +
		"    INNER JOIN diretorio d ON a.diretorio_pai_id = d.id " +
		"    INNER JOIN commite c ON d.commite_id = c.id " +
		"    INNER JOIN repositorio r ON c.repositorio_origem_id = r.id " +
		"    WHERE r.id = :repId " +
		"    GROUP BY a.nome " +
		") " +
		"SELECT a.* " +
		"FROM arquivo a " +
		"INNER JOIN diretorio d ON a.diretorio_pai_id = d.id " +
		"INNER JOIN commite c ON d.commite_id = c.id " +
		"INNER JOIN LatestFiles lf ON a.nome = lf.nome AND c.id = lf.latest_commit_id " +
		"INNER JOIN repositorio r ON c.repositorio_origem_id = r.id " +
		"WHERE r.id = :repId " +
		"ORDER BY a.nome")
	public List<Arquivo> findLatestVersionsByRepositorio(Long repId);
	
	/**
	 * Busca a versão mais recente de um arquivo específico em um repositório.
	 */
	@Query(nativeQuery = true, value = 
		"SELECT TOP 1 a.* " +
		"FROM arquivo a " +
		"INNER JOIN diretorio d ON a.diretorio_pai_id = d.id " +
		"INNER JOIN commite c ON d.commite_id = c.id " +
		"INNER JOIN repositorio r ON c.repositorio_origem_id = r.id " +
		"WHERE r.id = :repId AND a.nome = :nomeArquivo " +
		"ORDER BY c.id DESC")
	public Arquivo findLatestVersionByNomeAndRepositorio(Long repId, String nomeArquivo);
}
