create database TrabalhoLabEngSw
go
use TrabalhoLabEngSw
go
-------------
use master
go
drop database TrabalhoLabEngSw
go
create database TrabalhoLabEngSw
go
use TrabalhoLabEngSw
--------------

create table usuario (
	id				bigint			not null,
	nickname		varchar(15)		not null,
	senha			varchar(100)	not null,
	email			varchar(100)	not null,
	chave_publica	varbinary(max)	null
	primary key (id)
)

go

create table repositorio (
	id				bigint			not null,
	nome			varchar(50)	not null,
	chave_simetrica	varbinary(max)	not null,
	usuario_dono_id	bigint	not null
	primary key(id)
	foreign key(usuario_dono_id) references usuario(id)
)

go

create table commite (
	id						bigint			not null,
	msg						varchar(100)	not null,
	usuario_autor_id		bigint			not null,
	commite_anterior_id		bigint			null,
	repositorio_origem_id	bigint			not null
	primary key(id)
	foreign key(usuario_autor_id) references usuario(id),
	foreign key(commite_anterior_id) references commite(id),
	foreign key(repositorio_origem_id) references repositorio(id)
)

go

create table diretorio ( 
	id					bigint			not null,
	nome				varchar(50)	not null,
	commite_id			bigint			not null,
	diretorio_pai_id	bigint			null
	primary key(id)
	foreign key(commite_id) references commite(id),
	foreign key(diretorio_pai_id) references diretorio(id)
)

go

create table blob (
	id			bigint			not null,
	sha1		varchar(40)		not null,
	conteudo	varbinary(max)		not null
	primary key (id)
)

go

create table arquivo (
	id					bigint			not null,
	nome				varchar(100)	not null,
	blob_id				bigint			not null,
	diretorio_pai_id	bigint			not null
	primary key(id)
	foreign key(blob_id) references blob(id),
	foreign key(diretorio_pai_id) references diretorio(id)
)

go

-------------------------------------------------------------------------

go

create procedure sp_commit(@opc char(1), @id int, @msg varchar(100), @usuarioAutor int, @commiteAnterior int, @saida varchar(100) output)
as
begin
	if (upper(@opc) = 'I')
	begin
		if (@id is not null and @usuarioAutor is not null)
		begin
			insert into commite values
			(@id, @msg, @usuarioAutor, @commiteAnterior)
			set @saida = 'Commit #'+cast(@id as varchar(10))+' inserido com sucesso.'
		end
		else
		begin
			raiserror('Erro ao Inserir Commit: um ou mais campos obrigatorios estao em branco', 16, 1)
		end
	end
	else
	begin
		raiserror('Erro em sp_commit: Opcao Invalida', 16, 1)
	end
end

select id as usuario, nickname from usuario
select id as repositorio, nome, chave_simetrica, usuario_dono_id from repositorio
select id as commite, msg, commite_anterior_id as commit_anterior, usuario_autor_id, repositorio_origem_Id from commite
select id as diretorio, nome, commite_id, diretorio_pai_id from diretorio
select id as arquivo, nome, blob_id, diretorio_pai_id from arquivo
select id as blob, conteudo, sha1 id_sha1 from blob

--selecionar todos os arquivos de um repositório
select a.nome from arquivo a
inner join diretorio d on a.diretorio_pai_id = d.id
inner join commite c on d.commite_id = c.id
inner join repositorio r on c.repositorio_origem_id = r.id
where r.id = 49

--selecionar a versão mais recente de um arquivo de um repositório
select max(a.id) as id_mais_recente from arquivo a
inner join diretorio d on a.diretorio_pai_id = d.id
inner join commite c on d.commite_id = c.id
inner join repositorio r on c.repositorio_origem_id = r.id
where r.id = 49 and a.nome = 'upload tcc/teste.txt'

--selecionara a versão mais recente de todos os arquivos de um repositório
	WITH LatestFiles AS (SELECT a.nome, MAX(c.id) as latest_commit_id
		FROM arquivo a
		INNER JOIN diretorio d ON a.diretorio_pai_id = d.id
		INNER JOIN commite c ON d.commite_id = c.id
		INNER JOIN repositorio r ON c.repositorio_origem_id = r.id
		WHERE r.id = 49
		GROUP BY a.nome)
	SELECT a.*
	FROM arquivo a
	INNER JOIN diretorio d ON a.diretorio_pai_id = d.id
	INNER JOIN commite c ON d.commite_id = c.id
	INNER JOIN LatestFiles lf ON a.nome = lf.nome AND c.id = lf.latest_commit_id
	INNER JOIN repositorio r ON c.repositorio_origem_id = r.id
	WHERE r.id = 49
	ORDER BY a.nome

select * from arquivo 
delete from arquivo where id = 10155
select * from blob
delete from blob where id = 10115

select top 1 * from commite
order by id desc

insert into commite (msg, commite_anterior_id, usuario_autor_id, repositorio_origem_id) values
('hardcode no sql',1, 1, 1)


--ultimo commmit de determinado repositorio
select top 1 * from commite where repositorio_origem_id = 1
order by id desc

--arquivos do ultimo commit de determinado repositorio

select * from commite order by id asc