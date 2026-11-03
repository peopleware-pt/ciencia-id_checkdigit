# ciencia_id.js — contexto para continuar o trabalho

## Objetivo
Implementar e validar o identificador Ciência ID / Estudante-ID (12 dígitos hexadecimais),
com base no documento «Estudante-ID – Regras de uso do Identificador para Parceiros» v1.0 (MCTES, 2016).

## Estrutura do identificador (Tabela 4 – Estrutura Ciência-ID)
| Posição | Campo                | Comprimento | Tipo        | Notas |
|---------|----------------------|-------------|-------------|-------|
| 1–2     | Validação interna    | 2           | Hexadecimal | Mecanismo interno + SALT; só o emissor verifica |
| 3       | Entidade de registo  | 1           | Hexadecimal | Código da fonte |
| 4–11    | Identificador único  | 8           | «Numérico»  | Na prática hex (inteiro de 32 bits), não sequencial |
| 12      | Dígito de controlo   | 1           | Hexadecimal | ISO/IEC 7064, MOD 17,16 |

Comunicação: M2M sem hífens; leitura humana em grupos de 4 com hífens;
forma URI com prefixo de domínio seguido de «/».

## Decisões tomadas
- Algoritmo do dígito de controlo comum a Ciência ID e Estudante-ID.
- `normalize()` aceita os prefixos `ciencia-id.pt/` e `estudante-id.pt/` (com ou sem `https://`, `www.`).
- Dicionário de fontes `FONTES`: só a fonte «1» é conhecida (Ciência ID, `ciencia-id.pt`).
  Qualquer outra fonte é desconhecida e não recebe prefixo de domínio em `format()`.
- `parse()` devolve a fonte, o domínio indicado na entrada e `dominioCoerente`.
- Validação de entrada estrita (só hex), ao contrário do pseudo-código original (que só verificava o comprimento).

## API
`computeCheck(11 hex)`, `isValid(id)`, `parse(id)`, `build(validacao, entidade, uniqueNum)`,
`format(id, withDomain)`, `fonte(codigo)`, `dominioIndicado(str)`, `FONTES`.

## Validação feita
- 12 Ciência IDs públicos do CIÊNCIAVITAE: 12/12 válidos (todos com entidade = 1):
  EA11-91C2-39D1, 0F14-AD98-862F, 1C11-C5F1-BC01, E213-EBEB-1554, 521E-B8E0-5FC1, 3D1F-B551-CC8A,
  2215-8523-AF34, A81C-A16C-1048, BB15-6843-1448, E610-EAD3-992A, 2110-8993-7817, 2F1B-E8FA-5406
- Controlo negativo: 2279/2280 erros simples e transposições adjacentes detetados.
  Única falha: transposição entre o último dígito de dados e o dígito de controlo
  (1C11-C5F1-BC01 → …BC10) — limitação da norma MOD 17,16.

## Pendentes possíveis
- ~~Renomear o objeto exportado `EstudanteID` → `CienciaID`~~ (feito 2026-11-03).
- Acrescentar a `FONTES` o código do Estudante-ID quando for atribuído (confirmar com a FCCN).
- Separar os testes para um ficheiro próprio (`ciencia_id.test.js`).

Executar testes: `node ciencia_id.js`
