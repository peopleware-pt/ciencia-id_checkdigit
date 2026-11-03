// Estudante-ID / Ciência ID — dígito de validação ISO/IEC 7064 MOD 17,16
// Implementação fiel ao pseudo-código do documento «Estudante-ID – Regras de uso do Identificador para Parceiros» v1.0

const HEXA = 16, ModH = 17, ModL = 16;

// Dicionário de fontes (campo «Entidade de Registo», 3.ª posição).
// Neste momento só a fonte 1 é conhecida. Qualquer outra é desconhecida
// e não lhe é atribuído prefixo de domínio.
const FONTES = Object.freeze({
  "1": Object.freeze({ nome: "Ciência ID", dominio: "ciencia-id.pt" }),
});

// Prefixos de domínio aceites na entrada (algoritmo comum)
const RE_DOMINIO = /^(?:HTTPS?:\/\/)?(?:WWW\.)?((?:ESTUDANTE|CIENCIA)-ID\.PT)\//;

const CienciaID = {
  FONTES,

  f(x) { const v = x % ModL; return v === 0 ? ModL : v; },

  normalize(str) {
    return String(str).trim().toUpperCase().replace(RE_DOMINIO, "").replace(/-/g, "");
  },

  // Domínio indicado na entrada (em minúsculas) ou null
  dominioIndicado(str) {
    const m = String(str).trim().toUpperCase().match(RE_DOMINIO);
    return m ? m[1].toLowerCase() : null;
  },

  // Informação da fonte a partir do código de entidade
  fonte(codigo) {
    const f = FONTES[String(codigo).toUpperCase()];
    return f ? { codigo, conhecida: true, ...f }
             : { codigo, conhecida: false, nome: "Desconhecida", dominio: null };
  },

  // Recebe os 11 primeiros dígitos hex; devolve o dígito de validação (ou false)
  computeCheck(str) {
    str = this.normalize(str);
    if (!/^[0-9A-F]{11}$/.test(str)) return false;
    let t = ModL;
    for (let i = 0; i < 11; ++i) {
      const c = parseInt(str.charAt(i), HEXA);
      t = (2 * this.f(t + c)) % ModH;
    }
    return ((ModH - t) % ModL).toString(HEXA).toUpperCase();
  },

  // Valida um identificador completo de 12 dígitos
  isValid(str) {
    const s = this.normalize(str);
    if (!/^[0-9A-F]{12}$/.test(s)) return false;
    return this.computeCheck(s.slice(0, 11)) === s.charAt(11);
  },

  // Decomposição segundo a estrutura Ciência-ID (Tabela 4):
  // [0-1] validação interna (2 hex, SALT) | [2] entidade de registo (1 hex)
  // [3-10] identificador único (8 hex = inteiro de 32 bits) | [11] dígito de controlo
  parse(str) {
    const s = this.normalize(str);
    if (!this.isValid(s)) return null;
    const fonte = this.fonte(s.charAt(2));
    const indicado = this.dominioIndicado(str);
    return {
      validacaoInterna: s.slice(0, 2),
      entidadeRegisto: s.charAt(2),
      fonte,
      dominioIndicado: indicado,
      // coerente: sem domínio indicado, ou domínio indicado igual ao da fonte
      dominioCoerente: indicado === null || indicado === fonte.dominio,
      identificadorUnico: s.slice(3, 11),
      identificadorUnicoDec: parseInt(s.slice(3, 11), HEXA),
      digitoControlo: s.charAt(11),
    };
  },

  // Constrói um identificador a partir dos campos (a validação interna/SALT é do emissor)
  build(validacaoInterna, entidade, uniqueNum) {
    const body = validacaoInterna.toUpperCase().padStart(2, "0")
      + entidade.toString(HEXA).toUpperCase()
      + uniqueNum.toString(HEXA).toUpperCase().padStart(8, "0");
    return body + this.computeCheck(body);
  },

  // Formatação para leitura humana: XXXX-XXXX-XXXX.
  // Com withDomain, o prefixo vem do dicionário de fontes; fonte desconhecida → sem prefixo.
  format(str, withDomain = false) {
    const s = this.normalize(str);
    if (!/^[0-9A-F]{12}$/.test(s)) return null;
    const f = s.match(/.{4}/g).join("-");
    const dominio = withDomain ? this.fonte(s.charAt(2)).dominio : null;
    return dominio ? dominio + "/" + f : f;
  },
};

if (typeof module !== "undefined") module.exports = CienciaID;

// ---- Teste com Ciência IDs públicos do CIÊNCIAVITAE ----
if (require.main === module) {
  const ids = [
    "EA11-91C2-39D1", "0F14-AD98-862F", "1C11-C5F1-BC01", "E213-EBEB-1554",
    "521E-B8E0-5FC1", "3D1F-B551-CC8A", "2215-8523-AF34", "A81C-A16C-1048",
    "BB15-6843-1448", "E610-EAD3-992A", "2110-8993-7817", "2F1B-E8FA-5406",
  ];
  let ok = 0;
  for (const id of ids) {
    const s = CienciaID.normalize(id);
    const calc = CienciaID.computeCheck(s.slice(0, 11));
    const v = CienciaID.isValid(id);
    ok += v;
    console.log(`${id}  esperado=${s[11]}  calculado=${calc}  ${v ? "OK" : "FALHA"}`);
  }
  console.log(`\n${ok}/${ids.length} válidos`);

  // Controlo negativo: alterar 1 dígito ou trocar 2 adjacentes deve falhar
  let det = 0, tot = 0;
  for (const id of ids) {
    const s = CienciaID.normalize(id);
    for (let i = 0; i < 12; i++) for (const d of "0123456789ABCDEF") {
      if (d === s[i]) continue;
      tot++; if (!CienciaID.isValid(s.slice(0, i) + d + s.slice(i + 1))) det++;
    }
    for (let i = 0; i < 11; i++) {
      if (s[i] === s[i + 1]) continue;
      tot++; if (!CienciaID.isValid(s.slice(0, i) + s[i + 1] + s[i] + s.slice(i + 2))) det++;
    }
  }
  console.log(`Erros simples/transposições detetados: ${det}/${tot}`);
  console.log("Formato URI:", CienciaID.format(ids[0], true));

  console.log("\nDecomposição (Tabela 4):");
  for (const id of ids) {
    const p = CienciaID.parse(id);
    console.log(`${id}  val=${p.validacaoInterna} ent=${p.entidadeRegisto} uid=${p.identificadorUnico} (${p.identificadorUnicoDec})`);
  }
  const ents = new Set(ids.map(i => CienciaID.parse(i).entidadeRegisto));
  console.log("Entidades de registo observadas:", [...ents].join(", "));
  const b = CienciaID.build("EA", 1, 0x191C239D);
  console.log("build() reconstrói EA11-91C2-39D1:", CienciaID.format(b), b === "EA1191C239D1" ? "OK" : "FALHA");
}
