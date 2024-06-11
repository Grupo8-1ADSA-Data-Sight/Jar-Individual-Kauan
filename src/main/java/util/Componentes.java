package util;

import bancoDeDados.ConexaoServer;
import com.github.britooo.looca.api.core.Looca;
import com.github.britooo.looca.api.group.discos.Volume;
import com.github.britooo.looca.api.util.Conversor;
import org.json.JSONObject;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import java.util.List;

public class Componentes extends Looca {
    Maquina maquina = new Maquina();

    private Integer idCustomizacao;
    private Double usoCPU;
    private Double usoHD;
    private Double usoRAM;
    private Integer fkEmpresa;

    public Componentes() throws IOException {
    }

    public Componentes(Maquina maquina, Integer idCustomizacao, Double usoCPU, Double usoHD, Double usoRAM, Integer fkEmpresa) throws IOException {
        this.maquina = maquina;
        this.idCustomizacao = idCustomizacao;
        this.usoCPU = usoCPU;
        this.usoHD = usoHD;
        this.usoRAM = usoRAM;
        this.fkEmpresa = fkEmpresa;
    }

    public Boolean getPermissao() {
        return super.getSistema().getPermissao();
    }

    // METODOS:

    // Metodos de listas:
    public String listaArmazenamento() {
        String informacoesArmazenamento = null;

        for (int i = 0; i < getGrupoDeDiscos().getVolumes().size(); i++) {
            informacoesArmazenamento = String.format("""
                            Disco (%d)
                            %s
                            Espaço disponivel do disco = %.1f GiB
                            Espaço total do disco = %.1f GiB
                            """,
                    (i + 1),
                    getGrupoDeDiscos().getVolumes().get(i).getNome(),
                    (getGrupoDeDiscos().getVolumes().get(i).getDisponivel() / 1e+9),
                    (getGrupoDeDiscos().getVolumes().get(i).getTotal() / 1e+9)
            );

        }
        return informacoesArmazenamento;
    }

    public Double totalArmazenamentoDisponivel() {
        Double totalArmazenamentoDisponivel = 0.0;

        for (int i = 0; i < getGrupoDeDiscos().getVolumes().size(); i++) {
            totalArmazenamentoDisponivel += getGrupoDeDiscos().getVolumes().get(i).getDisponivel();
        }
        return totalArmazenamentoDisponivel / 1e+9;
    }

    public Double totalArmazenamento() {
        Double totalArmazenamento = 0.0;

        for (int i = 0; i < getGrupoDeDiscos().getVolumes().size(); i++) {
            totalArmazenamento += getGrupoDeDiscos().getVolumes().get(i).getTotal();
        }
        return totalArmazenamento / 1e+9;
    }

    public Double disponivelHd(){
        Double disponivelHd = 0.0;
        for (Volume volume : getGrupoDeDiscos().getVolumes()) {
            disponivelHd += volume.getDisponivel();
        }
        return disponivelHd / 1e+9;
    }

    public Double emUsoHD(){
        return (totalArmazenamento()  - totalArmazenamentoDisponivel());
    }


    // Metodos para apresentar
    public String exibirComponentesEstaticos() {
        StringBuilder sb = new StringBuilder();
        maquina.isLoginMaquina();


        return String.format("""
                                                                                
                                                                                
                                                                      Sistema
                        ------------------------------------------------------------------------------------------------                               
                        Sistema Operacional = %s
                        Fabricante = %s
                        Arquitetura = x%s
                        Permissões = %s
                        fkMaquina = %s                   
                        ------------------------------------------------------------------------------------------------
                                                                                
                                                                       CPU
                        ------------------------------------------------------------------------------------------------                              
                        idCpuMaquina = %s
                        Fabricante = %s
                        Nome = %s
                        Identificador = %s
                        FrequenciaGhz = %d
                        Nucleos Fisicos = %d
                        Nucleos Logicos = %d
                        fkMaquina = %s                        
                        ------------------------------------------------------------------------------------------------
                               
                                                                  Armazenamento
                        ------------------------------------------------------------------------------------------------
                                                
                         %s
                        Espaço Disponivel geral = %.1f GiB
                        Espaço total geral = %.1f GiB
                        fkMaquina = %s 
                        ------------------------------------------------------------------------------------------------
                                   
                                                                       RAM
                        ------------------------------------------------------------------------------------------------
                                                
                        Tamanho= %s
                        fkMaquina = %s 
                        ------------------------------------------------------------------------------------------------
                                                
                                                                       Rede
                        ------------------------------------------------------------------------------------------------
                        hostName= %s 
                        modelo= %s
                        ipv4= %s
                        fkMaquina= %s
                                  """,
                // Dados sistema
                super.getSistema().getSistemaOperacional(),
                super.getSistema().getFabricante(),
                super.getSistema().getArquitetura(),
                sb.append(("Executando como ")).append((this.getPermissao() ? "root" : "usuário padrão")),
                maquina.getIdMaquina(),

                // Dados processador (CPU)

                super.getProcessador().getId(),
                super.getProcessador().getFabricante(),
                super.getProcessador().getNome(),
                super.getProcessador().getIdentificador(),
                super.getProcessador().getFrequencia(),
                super.getProcessador().getNumeroCpusFisicas(),
                super.getProcessador().getNumeroCpusLogicas(),
                maquina.getIdMaquina(),

                // Dados armazenamento (HD/SD)

                listaArmazenamento(),
                totalArmazenamentoDisponivel(),
                totalArmazenamento() ,
                maquina.getIdMaquina(),


                // Dados memória RAM

                Conversor.formatarBytes(getMemoria().getTotal()),
                maquina.getIdMaquina(),

                // Dados Rede
                super.getRede().getParametros().getHostName(),
                super.getRede().getGrupoDeInterfaces().getInterfaces().get(1).getNomeExibicao(),
                super.getRede().getGrupoDeInterfaces().getInterfaces().get(1).getEnderecoIpv4(),
                maquina.getIdMaquina()
        );
    }

    public String exibirLeituraComponentes() throws IOException, InterruptedException {
        Double porcentagemDeUsoRAM = (double) super.getMemoria().getEmUso() / super.getMemoria().getTotal() * 100;
        Double porcentagemDeUsoHD = (double) emUsoHD() / (totalArmazenamento()) * 100;
        // convetendo disponivel da memória RAM
        Double disponivelEmGibRAM = super.getMemoria().getDisponivel() / 1e+9;
        JSONObject json = new JSONObject();
        ConexaoServer conexaoServer = new ConexaoServer();
        JdbcTemplate conServer = conexaoServer.getConexaoDoBancoServer();

        List<Componentes> customizados = conServer.query("Select * from customizacoes", new BeanPropertyRowMapper<>(Componentes.class));

        for (Componentes customizado : customizados) {
            if (customizado.getFkEmpresa().equals(maquina.getFkEmpresa())) {
                if (getProcessador().getUso() > customizado.getUsoCPU()) {
                     json.put("text",(String.format("""
                    Uso da CPU acima de %.2f%%, manter alerta!!""", getProcessador().getUso())));
                     Slack.sendMessage(json);
                }

                if (porcentagemDeUsoHD > customizado.getUsoHD()) {
                     json.put("text", (String.format("""
                    Uso do disco acima de %.2f%%, manter alerta!!""", porcentagemDeUsoHD)));
                     Slack.sendMessage(json);
                }

                if (porcentagemDeUsoRAM > customizado.getUsoRAM()) {
                     json.put("text", (String.format("""
                    Uso da memória ram acima de %.2f%%, manter alerta!!""", porcentagemDeUsoRAM)));
                     Slack.sendMessage(json);
                }
            }
        }

        return String.format("""
                           |--------------------------|
                           |           CPU            |
                           |--------------------------|
                           |    Uso   |   Frequência  |
                           |                          |
                           |   %.1f%%  |    %.1fGhz      |
                           |__________________________|
                           
                           |--------------------------|
                           |      Armazenamento       |
                           |--------------------------|
                           |     Uso   |   Disponível |
                           |                          |
                           |  %.1f%%   |   %.1f GiB  |
                           |__________________________|
                                        
                           |--------------------------|
                           |      Memória Ram         |
                           |--------------------------|
                           |      USO   |  Disponível |
                           |                          |
                           |    %.1f%%   |  %.1f Gib    |
                           |__________________________|
                           
                          
                        %n""",
                super.getProcessador().getUso(),
                super.getProcessador().getFrequencia() / 1e+9,
                porcentagemDeUsoHD,
                totalArmazenamentoDisponivel(),
                porcentagemDeUsoRAM,
                disponivelEmGibRAM);
    }

    public Maquina getMaquina() {
        return maquina;
    }

    public void setMaquina(Maquina maquina) {
        this.maquina = maquina;
    }

    public Integer getIdCustomizacao() {
        return idCustomizacao;
    }

    public void setIdCustomizacao(Integer idCustomizacao) {
        this.idCustomizacao = idCustomizacao;
    }

    public Double getUsoCPU() {
        return usoCPU;
    }

    public void setUsoCPU(Double usoCPU) {
        this.usoCPU = usoCPU;
    }

    public Double getUsoHD() {
        return usoHD;
    }

    public void setUsoHD(Double usoHD) {
        this.usoHD = usoHD;
    }

    public Double getUsoRAM() {
        return usoRAM;
    }

    public void setUsoRAM(Double usoRAM) {
        this.usoRAM = usoRAM;
    }

    public Integer getFkEmpresa() {
        return fkEmpresa;
    }

    public void setFkEmpresa(Integer fkEmpresa) {
        this.fkEmpresa = fkEmpresa;
    }
}


