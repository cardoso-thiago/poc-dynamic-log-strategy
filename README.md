# Dynamic Log Level

Este repositório contém a implementação de um mecanismo dinâmico para ajuste de nível de log baseado na taxa de sucesso de eventos de validação em um sistema Spring Boot.

## Benefícios

1. **Ajuste Dinâmico de Log**:
    - Ajusta dinamicamente o nível de log com base na taxa de sucesso dos eventos de validação, proporcionando mais informações detalhadas para diagnóstico em momentos de alta taxa de erro.

2. **Configuração Flexível**:
    - Permite configurar vários parâmetros via propriedades do ambiente, tornando o sistema adaptável a diferentes necessidades e cenários operacionais.

3. **Redução de Ruído de Log**:
    - Opera com um nível de log mais alto (como ERROR) em situações normais, reduzindo o volume de logs gerados e melhorando a performance.

4. **Automatização de Monitoramento**:
    - Automatiza o ajuste do nível de log com base na taxa de sucesso dos eventos, permitindo uma reação mais rápida a problemas sem necessidade de intervenção manual.

## Usos

1. **Ambientes de Produção de Alta Demanda**:
    - Útil para sistemas com muitas requisições por segundo, onde é crucial identificar rapidamente problemas sem gerar muitos logs desnecessários.

2. **Monitoramento de Serviços Críticos**:
    - Valioso para serviços que requerem alta disponibilidade e onde a análise de logs detalhados é essencial durante falhas.

3. **Sistemas de Microserviços**:
    - Ajuda a manter uma visão clara do estado de saúde de cada serviço individualmente em arquiteturas de microserviços.

4. **Ambientes de Desenvolvimento e Teste**:
    - Auxilia desenvolvedores a identificar e resolver problemas rapidamente, aumentando a produtividade e a qualidade do software.

## Considerações Adicionais

1. **Impacto na Performance**:
    - Monitorar o impacto na performance do sistema, especialmente em ambientes de alta demanda.

2. **Manutenção de Eventos de Validação**:
    - A remoção periódica de eventos antigos é crucial para evitar o consumo excessivo de memória.

3. **Teste e Validação**:
    - Testar o mecanismo em diferentes cenários e cargas de trabalho é essencial para garantir seu comportamento conforme esperado.

4. **Configuração Inicial**:
    - A configuração inicial dos thresholds e outros parâmetros deve ser feita com base em um entendimento detalhado do comportamento do sistema sob diferentes condições de carga e taxa de erro.
   
5. **Sensibilidade das mudanças**:
   - Os valores iniciais e mudanças de acordo com o RPS precisam ser avaliados em mais cenários, pois a mudança de um level para outro pode ficar muito sensível e desencadear mudanças constantes em um cenário de alta carga.

## Configuração

As seguintes propriedades podem ser configuradas no ambiente para ajustar o comportamento do mecanismo:

- `validation.window.seconds`: Janela de validação em segundos (default: 30)
- `error.threshold`: Threshold para o nível de log ERROR (default: 97)
- `warning.threshold`: Threshold para o nível de log WARN (default: 95)
- `info.threshold`: Threshold para o nível de log INFO (default: 90)
- `debug.enabled`: Habilita ou desabilita o nível de log DEBUG (default: false)
- `active.errors.debug`: Número de erros ativos para habilitar o nível de log DEBUG (default: 5)
- `starting.log.level`: Nível de log inicial (default: ERROR)

### Mecanismo de RPS (Requests Per Second)

O mecanismo de RPS monitora a taxa de requisições por segundo e ajusta dinamicamente a janela de validação e os thresholds de log com base nessa métrica. Isso é particularmente útil para adaptar o sistema às variações na carga de trabalho e garantir que os logs sejam ajustados de forma apropriada.

#### Propriedades do RPS

- `high.rps`: Define o limite superior de RPS. Se o RPS médio exceder esse valor, a janela de validação é reduzida para melhorar a responsividade do ajuste de log. Os thresholds de validação da mudança do level também são ajustados. (default: 1000)
- `medium.rps`: Define o limite intermediário de RPS. Se o RPS médio estiver entre `medium.rps` e `high.rps`, a janela de validação é ajustada de forma moderada. Os thresholds de validação da mudança do level também são ajustados. (default: 500)

## Uso

### Adicionando um Evento de Validação

Para adicionar um evento de validação, use o método `addValidationEvent`:

```java
dynamicLogLevel.addValidationEvent(true); // Evento de sucesso
dynamicLogLevel.addValidationEvent(false); // Evento de falha
```

## Referência

A implementação deste mecanismo foi inspirada pela ideia apresentada no artigo [Log Adaptativo: Revolucionando a captura de logs e FinOps](https://www.linkedin.com/pulse/log-adaptativo-revolucionando-captura-de-logs-e-finops-braga-lk9jf/).
