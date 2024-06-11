package bancoDeDados;

import com.github.britooo.looca.api.core.Looca;
import com.github.britooo.looca.api.group.discos.Volume;
import com.github.britooo.looca.api.util.Conversor;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Componentes;
import util.Log;
import util.Maquina;

import java.io.IOException;

public class InserirDadosNaTabela {
    Conexao conexao = new Conexao();
    JdbcTemplate con = conexao.getConexaoDoBanco();
    ConexaoServer conexaoServer = new ConexaoServer();
    JdbcTemplate conServer = conexaoServer.getConexaoDoBancoServer();
    Looca looca = new Looca();
    Maquina maquina = new Maquina();
    Componentes componentes = new Componentes();
    Log logger = new Log();

    public InserirDadosNaTabela() throws IOException {
        logger.createLog();
        logger.writeLog("Construtor InserirDadosNaTabela chamado");
        logger.closeLog();
    }

    private boolean dadosExistemSql(String sql, Object... params) throws IOException {
        logger.writeLog("Verificando se os dados existem no servidor SQL: " + sql);
        Integer count = conServer.queryForObject(sql, params, Integer.class);
        logger.writeLog("Resultado da verificação: " + (count == null ? "null" : count));
        return count == null || count <= 0;
    }

    private boolean dadosExistemMysql(String sql, Object... params) throws IOException {
        logger.writeLog("Verificando se os dados existem no MySQL: " + sql);
        Integer count = con.queryForObject(sql, params, Integer.class);
        logger.writeLog("Resultado da verificação: " + (count == null ? "null" : count));
        return count == null || count <= 0;
    }

    public void inserirDadosFixos() throws IOException {
        logger.createLog();
        logger.writeLog("Método inserirDadosFixos iniciado");

        maquina.isLoginMaquina();
        logger.writeLog("Login da máquina verificado");

        // Inserindo no banco de dados da CPU, puxando os dados pela API - looca
        String hostName = looca.getRede().getParametros().getHostName();
        logger.writeLog("Hostname: " + hostName);


        if (dadosExistemSql("SELECT COUNT(*) FROM cpu join Maquina on fkMaquina = idMaquina WHERE hostName = ?", looca.getRede().getParametros().getHostName())) {
            logger.writeLog("Dados de CPU não existem no servidor, inserindo dados...");
            conServer.update("INSERT INTO CPU (fabricante, nome, identificador, frequenciaGHz, fkMaquina) values (?, ?, ?, ?, ?)", looca.getProcessador().getFabricante(), looca.getProcessador().getNome(), looca.getProcessador().getIdentificador(), looca.getProcessador().getFrequencia(), maquina.getIdMaquina());
        }

        // Inserindo no banco de dados da HD, puxando os dados pela API - looca
        for (Volume volume : looca.getGrupoDeDiscos().getVolumes()) {
            String volumeNome = volume.getNome();
            logger.writeLog("Verificando volume: " + volumeNome);

            if (dadosExistemSql("SELECT COUNT(*) FROM HD WHERE nome = ? AND fkMaquina = ?", volume.getNome(), maquina.getIdMaquina())) {
                logger.writeLog("Dados de HD não existem no servidor, inserindo dados...");
                conServer.update("INSERT INTO HD (nome, tamanho, fkMaquina) values (?, ? , ?)", volume.getNome(), (volume.getTotal() / 1e+9), maquina.getIdMaquina());
            }
        }

        // Inserindo no banco de dados da RAM, puxando os dados pela API - looca
        if (dadosExistemSql("SELECT COUNT(*) FROM RAM WHERE fkMaquina = ?", maquina.getIdMaquina())) {
            logger.writeLog("Dados de RAM não existem no servidor, inserindo dados...");
            conServer.update("INSERT INTO RAM (armazenamentoTotal, fkMaquina) values (?, ?)", looca.getMemoria().getTotal(), maquina.getIdMaquina());
        }

        if(dadosExistemMysql("SELECT COUNT(*) FROM CPU WHERE idCPU = 1")){
            logger.writeLog("Dados de CPU não existem no MySQL, inserindo dados...");
            con.update("INSERT INTO CPU (fabricante, nome, identificador, frequenciaGHz, fkMaquina) values (?, ?, ?, ?, ?)", looca.getProcessador().getFabricante(), looca.getProcessador().getNome(), looca.getProcessador().getIdentificador(), looca.getProcessador().getFrequencia(), maquina.getIdMaquina());

            for (Volume volume : looca.getGrupoDeDiscos().getVolumes()) {
                logger.writeLog("Dados de HD não existem no servidor, inserindo dados...");
                con.update("INSERT INTO HD (nome, tamanho, fkMaquina) values (?, ? , ?)", volume.getNome(), (volume.getTotal() / 1e+9), maquina.getIdMaquina());
            }
            logger.writeLog("Dados de RAM não existem no MySQL, inserindo dados...");
            con.update("INSERT INTO RAM (armazenamentoTotal, fkMaquina) values (?, ?)", (looca.getMemoria().getTotal() / 1e+9), maquina.getIdMaquina());
        }

        logger.writeLog("Dados fixos da CPU, Memória RAM, Disco e Rede enviados");
        logger.closeLog();
    }

    public void inserindoDadosDinamicos() throws IOException {
        final StringBuilder sb = new StringBuilder();
        logger.createLog();
        logger.writeLog("Método inserindoDadosDinamicos iniciado");

        // Inserindo no banco de dados da CPULeitura, puxando os dados pela API - looca
        logger.writeLog("Inserindo dados de leitura da CPU");
        con.update("INSERT INTO CPULeitura (uso, tempoAtividade, dataHoraLeitura, fkCPU) values (?, ?, now(), (select max(idcpu) from CPU))", looca.getProcessador().getUso(), (sb.append("")
                .append(Conversor.formatarSegundosDecorridos(looca.getSistema().getTempoDeAtividade()))));
        conServer.update("INSERT INTO CPULeitura (uso, tempoAtividade, dataHoraLeitura, fkCPU) values (?, ?, GETDATE(), (select max(idcpu) from CPU))", looca.getProcessador().getUso(), (sb.append("")
                .append(Conversor.formatarSegundosDecorridos(looca.getSistema().getTempoDeAtividade()))));

        // Inserindo no banco de dados da HDLeitura, puxando os dados pela API - looca
        logger.writeLog("Inserindo dados de leitura do HD");
        for (Volume volume : looca.getGrupoDeDiscos().getVolumes()) {
            con.update("INSERT INTO HDLeitura (uso, disponivel, dataHoraLeitura, fkHD) values (?, ?, now(), (select max(idHD) from HD))", componentes.emUsoHD(), componentes.disponivelHd());
            conServer.update("INSERT INTO HDLeitura (uso, disponivel, dataHoraLeitura, fkHD) values (?, ?, GETDATE(), (select max(idHD) from HD))", componentes.emUsoHD(), componentes.disponivelHd());
        }

        // Inserindo no banco de dados da RAMLeitura, puxando os dados pela API - looca
        logger.writeLog("Inserindo dados de leitura da RAM");
        con.update("INSERT INTO RAMLeitura (emUso, disponivel, dataHoraLeitura, fkRam) values (?, ?, now(), (select max(idRAM) from RAM))", (looca.getMemoria().getEmUso() / 1e+9), (looca.getMemoria().getDisponivel() / 1e+9));
        conServer.update("INSERT INTO RAMLeitura (emUso, disponivel, dataHoraLeitura, fkRam) values (?, ?, GETDATE(), (select max(idRAM) from RAM))", (looca.getMemoria().getEmUso() / 1e+9), (looca.getMemoria().getDisponivel() / 1e+9));

        logger.writeLog("Dados dinâmicos da CPU, Memória RAM, Disco e Rede enviados");
        logger.closeLog();
    }
}