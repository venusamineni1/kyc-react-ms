const pptxgen = require("/opt/homebrew/lib/node_modules/pptxgenjs");

// ── Muted colour palette ──────────────────────────────────────────────────────
const C = {
  bg:        "F4F5F7",
  titleBar:  "263446",
  strip:     "E9EBED",
  stripBdr:  "B0B7BF",
  gcpBdr:    "7A9EC0",
  white:     "FFFFFF",
  dark:      "2C3E50",
  mid:       "5F6B78",
  light:     "9AA4AE",
  compute:   "5578A0",
  computeD:  "415E82",
  computeBg: "E6EDF5",
  sec1:      "A85A50",
  sec2:      "9A524A",
  sec3:      "8F4D46",
  cicd:      "B8924A",
  cicdBdr:   "9A7A3A",
  db:        "706898",
  dbBdr:     "5C5480",
  cache:     "9E5656",
  cacheBdr:  "834646",
  stor:      "4D8A5C",
  storBdr:   "3C6E49",
  idFill:    "E8F3F5",
  idBdr:     "3D8090",
  idBox:     "3D8090",
  idBoxD:    "2E6372",
  asyncFill: "F5EFE6",
  asyncBdr:  "A8783C",
  pubsub:    "A8783C",
  sched:     "B8884C",
  obsFill:   "EAF3EC",
  obsBdr:    "457A52",
  obsBox:    "457A52",
  obsBoxD:   "355E3F",
  arrBlue:   "5578A0",
  arrSec:    "A85A50",
  arrOrange: "A87830",
  arrPurple: "706898",
  arrGreen:  "457A52",
  arrGold:   "B8924A",
};

let pres = new pptxgen();
pres.layout = 'LAYOUT_WIDE'; // 13.3" x 7.5"
pres.title  = 'KYC Orchestration — GCP Architecture';

// ── shared helpers ────────────────────────────────────────────────────────────
function titleBar(slide, left, right) {
  slide.addShape(pres.shapes.RECTANGLE, { x:0, y:0, w:13.3, h:0.42, fill:{ color: C.titleBar }, line:{ color: C.titleBar } });
  slide.addText(left,  { x:0.15, y:0, w:9,   h:0.42, fontSize:12, bold:true, color:"D0DCE8", valign:"middle", fontFace:"Calibri", margin:0 });
  slide.addText(right, { x:9.2,  y:0, w:3.9, h:0.42, fontSize:9,  color:"7A9EC0", valign:"middle", align:"right", fontFace:"Calibri", margin:0 });
}

// ═══════════════════════════════════════════════════════════════════════════════
// SLIDE 1 — Title
// ═══════════════════════════════════════════════════════════════════════════════
let s1 = pres.addSlide();
s1.background = { color: "1E2A3A" };
s1.addShape(pres.shapes.RECTANGLE, { x:0, y:0,    w:13.3, h:0.07, fill:{ color: C.gcpBdr }, line:{ color: C.gcpBdr } });
s1.addShape(pres.shapes.RECTANGLE, { x:0, y:7.43, w:13.3, h:0.07, fill:{ color: C.gcpBdr }, line:{ color: C.gcpBdr } });
[["A85A50",1.0],["B8924A",1.28],["4D8A5C",1.56],[C.gcpBdr,1.84]].forEach(([c,x])=>
  s1.addShape(pres.shapes.OVAL,{ x, y:1.1, w:0.15, h:0.15, fill:{ color:c }, line:{ color:c } }));
s1.addText("KYC Orchestration", { x:0.8, y:2.1,  w:11.7, h:1.0, fontSize:50, bold:true, color:"D8E4F0", align:"center", fontFace:"Calibri", margin:0 });
s1.addText("GCP Architecture",  { x:0.8, y:3.05, w:11.7, h:0.9, fontSize:50, bold:true, color: C.gcpBdr, align:"center", fontFace:"Calibri", margin:0 });
s1.addShape(pres.shapes.RECTANGLE, { x:3.8, y:4.1, w:5.7, h:0.04, fill:{ color: C.gcpBdr }, line:{ color: C.gcpBdr } });
s1.addText("Security Design Authority Review", { x:0.8, y:4.25, w:11.7, h:0.55, fontSize:20, color:"8AAAC8", align:"center", fontFace:"Calibri", margin:0 });
s1.addText("April 2026",                       { x:0.8, y:4.9,  w:11.7, h:0.45, fontSize:14, color:"5A7A96", align:"center", fontFace:"Calibri", margin:0 });

// ═══════════════════════════════════════════════════════════════════════════════
// SLIDE 2 — Executive Summary
// ═══════════════════════════════════════════════════════════════════════════════
let s2 = pres.addSlide();
s2.background = { color: C.bg };
titleBar(s2, "Executive Summary", "Security Design Authority  |  April 2026");

// Purpose statement
s2.addShape(pres.shapes.RECTANGLE, { x:0.3, y:0.55, w:12.7, h:0.38, fill:{ color:"EBF0F5" }, line:{ color:"B0BEC8", width:1 } });
s2.addText("We are seeking Security Design Authority approval to deploy the KYC Orchestration Service on Google Cloud Platform in support of automated customer onboarding across EIS and InvestiFi channels.", {
  x:0.45, y:0.55, w:12.4, h:0.38, fontSize:9, color:"2C3E50", valign:"middle", italic:true, fontFace:"Calibri", margin:0
});

// Three-column cards: Context | Proposed Solution | Expected Outcome
const sumCols = [
  {
    title: "01  Background & Problem",
    color: C.sec1,
    points: [
      "KYC onboarding is currently a fragmented, manual process spanning multiple systems with no single audit record.",
      "Compliance teams carry the risk of incomplete or inconsistent screening data across NCA, NLS/NRTS and CRRE.",
      "Absence of a centralised orchestration layer creates delays of up to 5 business days per application and limits capacity to scale.",
    ]
  },
  {
    title: "02  Proposed Solution",
    color: C.compute,
    points: [
      "A new Spring Boot microservice — kyc-orchestration — will coordinate identity verification, sanctions screening and risk rating in a single automated workflow.",
      "All data will be held in an encrypted, private Cloud SQL instance; PII is encrypted at field level using AES-128/GCM before storage.",
      "The service is deployed on GKE Autopilot within a private VPC, protected by Cloud Armor WAF and OAuth 2.0 access controls.",
    ]
  },
  {
    title: "03  Expected Outcome",
    color: C.stor,
    points: [
      "Onboarding decision time reduced from ~5 days to same-session for the majority of low-risk applicants.",
      "Full immutable audit trail in BigQuery satisfies regulatory retention requirements (7 years) with no manual effort.",
      "Automated ON_HOLD flow for high-risk or watchlist-hit cases routes to the compliance team with structured data, reducing investigator effort.",
    ]
  },
];

sumCols.forEach((col, i) => {
  const cx = 0.30 + i * 4.33;
  const cw = 4.20;
  // Header
  s2.addShape(pres.shapes.RECTANGLE, { x:cx, y:1.05, w:cw, h:0.36, fill:{ color: col.color }, line:{ color: col.color } });
  s2.addText(col.title, { x:cx+0.10, y:1.05, w:cw-0.14, h:0.36, fontSize:8.5, bold:true, color:"F0F4F8", valign:"middle", fontFace:"Calibri", margin:0 });
  // Body card
  s2.addShape(pres.shapes.RECTANGLE, { x:cx, y:1.41, w:cw, h:5.30, fill:{ color: C.white }, line:{ color:"D0D8E0", width:1 } });
  s2.addShape(pres.shapes.RECTANGLE, { x:cx, y:1.41, w:0.04, h:5.30, fill:{ color: col.color }, line:{ color: col.color } });
  col.points.forEach((pt, pi) => {
    const py = 1.55 + pi * 1.65;
    // Numbered circle
    s2.addShape(pres.shapes.OVAL, { x:cx+0.14, y:py, w:0.22, h:0.22, fill:{ color: col.color }, line:{ color: col.color } });
    s2.addText(String(pi+1), { x:cx+0.14, y:py, w:0.22, h:0.22, fontSize:7, bold:true, color:"FFFFFF", align:"center", valign:"middle", fontFace:"Calibri", margin:0 });
    s2.addText(pt, { x:cx+0.42, y:py, w:cw-0.52, h:1.40, fontSize:8, color:"2C3E50", fontFace:"Calibri", margin:0 });
  });
});

// Bottom classification note
s2.addText("INTERNAL — CONFIDENTIAL", { x:0.3, y:6.80, w:12.7, h:0.20, fontSize:7, color:C.light, align:"center", fontFace:"Calibri", margin:0 });

// ═══════════════════════════════════════════════════════════════════════════════
// SLIDE 3 — Key Business Benefits
// ═══════════════════════════════════════════════════════════════════════════════
let s3 = pres.addSlide();
s3.background = { color: C.bg };
titleBar(s3, "Key Business Benefits", "Security Design Authority  |  April 2026");

const benefits = [
  {
    color:   C.compute,
    heading: "Regulatory Compliance",
    stat:    "7-Year",
    statSub: "Audit Retention",
    points: [
      "Immutable, tamper-evident audit trail stored in BigQuery for every KYC decision.",
      "Field-level PII encryption (AES-128/GCM) and structured data masking in all log output.",
      "Supports GDPR, AML and internal data classification policies out of the box.",
    ]
  },
  {
    color:   C.stor,
    heading: "Operational Efficiency",
    stat:    "< 30 s",
    statSub: "Automated Decision",
    points: [
      "Parallel orchestration of screening, risk and identity checks replaces a manual 5-day workflow.",
      "Automated webhook notifications alert downstream systems the moment a decision is reached.",
      "ON_HOLD cases are routed with full structured context, reducing investigator handling time.",
    ]
  },
  {
    color:   C.idBox,
    heading: "Security & Zero-Trust",
    stat:    "Zero",
    statSub: "Static Credentials in Pods",
    points: [
      "Workload Identity removes all static JSON key files; credentials are issued at runtime.",
      "VPC Service Controls create a data-exfiltration perimeter around all sensitive data stores.",
      "Cloud Armor WAF and OAuth 2.0 enforce defence-in-depth at every ingress point.",
    ]
  },
  {
    color:   C.cicd,
    heading: "Scalability & Resilience",
    stat:    "99.9%",
    statSub: "Uptime SLA (GCP)",
    points: [
      "GKE Autopilot scales from a minimum of 2 to 10 pods automatically based on load.",
      "Cloud SQL HA with a read replica and automated failover; no single point of failure.",
      "PodDisruptionBudget ensures rolling deployments never reduce capacity below baseline.",
    ]
  },
];

benefits.forEach((b, i) => {
  const col = i % 2;
  const row = Math.floor(i / 2);
  const bx  = 0.30 + col * 6.50;
  const by  = 0.58 + row * 3.20;
  const bw  = 6.25;
  const bh  = 3.05;

  // Card background
  s3.addShape(pres.shapes.RECTANGLE, { x:bx, y:by, w:bw, h:bh, fill:{ color: C.white }, line:{ color:"D0D8E0", width:1 } });
  // Top colour bar
  s3.addShape(pres.shapes.RECTANGLE, { x:bx, y:by, w:bw, h:0.08, fill:{ color: b.color }, line:{ color: b.color } });
  // Stat block (left)
  s3.addShape(pres.shapes.RECTANGLE, { x:bx, y:by+0.08, w:1.55, h:bh-0.08, fill:{ color: b.color }, line:{ color: b.color } });
  s3.addText(b.stat,    { x:bx+0.04, y:by+0.55, w:1.47, h:0.55, fontSize:22, bold:true, color:"FFFFFF", align:"center", fontFace:"Calibri", margin:0 });
  s3.addText(b.statSub, { x:bx+0.04, y:by+1.10, w:1.47, h:0.60, fontSize:7,  color:"E0EAF0", align:"center", fontFace:"Calibri", margin:0 });
  // Heading
  s3.addText(b.heading, { x:bx+1.65, y:by+0.12, w:bw-1.75, h:0.30, fontSize:10, bold:true, color:"2C3E50", fontFace:"Calibri", margin:0 });
  // Bullet points
  b.points.forEach((pt, pi) => {
    s3.addShape(pres.shapes.RECTANGLE, { x:bx+1.65, y:by+0.50+pi*0.76, w:0.06, h:0.06, fill:{ color: b.color }, line:{ color: b.color } });
    s3.addText(pt, { x:bx+1.78, y:by+0.44+pi*0.76, w:bw-1.90, h:0.72, fontSize:8, color:"3A4A58", fontFace:"Calibri", margin:0 });
  });
});

s3.addText("INTERNAL — CONFIDENTIAL", { x:0.3, y:6.80, w:12.7, h:0.20, fontSize:7, color:C.light, align:"center", fontFace:"Calibri", margin:0 });

// ═══════════════════════════════════════════════════════════════════════════════
// SLIDE 4 — Risk & Controls
// ═══════════════════════════════════════════════════════════════════════════════
let s4 = pres.addSlide();
s4.background = { color: C.bg };
titleBar(s4, "Risk Assessment & Controls", "Security Design Authority  |  April 2026");

s4.addText("The following risks have been identified and mitigated through the proposed architecture. Residual risk in each case is assessed as Low.", {
  x:0.30, y:0.50, w:12.7, h:0.30, fontSize:8.5, color:"3A4A58", italic:true, fontFace:"Calibri", margin:0
});

// Table header
const thY  = 0.88;
const thH  = 0.32;
const cols4 = [
  { label:"Risk",               x:0.30, w:2.40 },
  { label:"Category",           x:2.72, w:1.40 },
  { label:"Inherent Risk",      x:4.14, w:1.20 },
  { label:"Control Measures",   x:5.36, w:5.20 },
  { label:"Residual Risk",      x:10.58,w:1.20 },
  { label:"Owner",              x:11.80,w:1.20 },
];
cols4.forEach(c => {
  s4.addShape(pres.shapes.RECTANGLE, { x:c.x, y:thY, w:c.w, h:thH, fill:{ color: C.titleBar }, line:{ color:"FFFFFF", width:0.5 } });
  s4.addText(c.label, { x:c.x+0.06, y:thY, w:c.w-0.08, h:thH, fontSize:7.5, bold:true, color:"D0DCE8", valign:"middle", fontFace:"Calibri", margin:0 });
});

const risks = [
  {
    risk:     "PII Data Breach",
    cat:      "Security",
    inh:      "HIGH",
    inhColor: "A85A50",
    controls: "AES-128/GCM field-level encryption at rest; TLS 1.2+ in transit; VPC Service Controls perimeter; no public IPs on data services; PII masked in all log output.",
    res:      "LOW",
    resColor: "4D8A5C",
    owner:    "CISO",
  },
  {
    risk:     "Unauthorised Access",
    cat:      "Security",
    inh:      "HIGH",
    inhColor: "A85A50",
    controls: "OAuth 2.0 Client Credentials enforced at API Gateway; Workload Identity (no static keys); Cloud Armor WAF at ingress; least-privilege IAM roles per service account.",
    res:      "LOW",
    resColor: "4D8A5C",
    owner:    "CISO",
  },
  {
    risk:     "Service Unavailability",
    cat:      "Operational",
    inh:      "MEDIUM",
    inhColor: "B8924A",
    controls: "Cloud SQL HA with automated failover and read replica; GKE HPA min 2 pods; PodDisruptionBudget; 99.9% GCP uptime SLA; async Pub/Sub decouples downstream delivery.",
    res:      "LOW",
    resColor: "4D8A5C",
    owner:    "Engineering",
  },
  {
    risk:     "Regulatory Non-Compliance",
    cat:      "Compliance",
    inh:      "HIGH",
    inhColor: "A85A50",
    controls: "Immutable audit trail in BigQuery with 7-year retention; CMEK encryption satisfies data residency; automated ON_HOLD routing prevents un-reviewed high-risk approvals.",
    res:      "LOW",
    resColor: "4D8A5C",
    owner:    "Compliance",
  },
  {
    risk:     "Third-Party API Failure",
    cat:      "Operational",
    inh:      "MEDIUM",
    inhColor: "B8924A",
    controls: "CompletableFuture orchestration with independent timeouts per upstream (KYC-NCA, NLS, CRRE); exponential back-off retry (3 attempts); dead-letter queue for failed webhooks.",
    res:      "LOW",
    resColor: "4D8A5C",
    owner:    "Engineering",
  },
  {
    risk:     "Secret / Key Compromise",
    cat:      "Security",
    inh:      "MEDIUM",
    inhColor: "B8924A",
    controls: "All secrets held exclusively in Secret Manager (never in code or environment variables); Cloud KMS CMEK with automatic rotation; access audited via Cloud Logging.",
    res:      "LOW",
    resColor: "4D8A5C",
    owner:    "CISO",
  },
  {
    risk:     "Data Exfiltration",
    cat:      "Security",
    inh:      "MEDIUM",
    inhColor: "B8924A",
    controls: "VPC Service Controls access perimeter restricts egress; Workload Identity limits pod permissions to declared APIs only; all outbound calls are logged and alertable.",
    res:      "LOW",
    resColor: "4D8A5C",
    owner:    "CISO",
  },
];

const rowH4 = 0.72;
risks.forEach((r, i) => {
  const ry   = thY + thH + i * rowH4;
  const fill = i % 2 === 0 ? "FFFFFF" : "F0F2F4";
  // Row background
  s4.addShape(pres.shapes.RECTANGLE, { x:0.30, y:ry, w:12.70, h:rowH4, fill:{ color: fill }, line:{ color:"D8DADE", width:0.5 } });
  // Risk name
  s4.addText(r.risk, { x:0.36, y:ry+0.04, w:2.28, h:rowH4-0.08, fontSize:7.5, bold:true, color:"2C3E50", valign:"middle", fontFace:"Calibri", margin:0 });
  // Category
  s4.addText(r.cat,  { x:2.78, y:ry+0.04, w:1.28, h:rowH4-0.08, fontSize:7,   color:"3A4A58", valign:"middle", fontFace:"Calibri", margin:0 });
  // Inherent risk pill
  s4.addShape(pres.shapes.RECTANGLE, { x:4.20, y:ry+0.18, w:1.00, h:0.28, fill:{ color: r.inhColor }, line:{ color: r.inhColor } });
  s4.addText(r.inh, { x:4.20, y:ry+0.18, w:1.00, h:0.28, fontSize:7, bold:true, color:"FFFFFF", align:"center", valign:"middle", fontFace:"Calibri", margin:0 });
  // Controls
  s4.addText(r.controls, { x:5.42, y:ry+0.03, w:5.08, h:rowH4-0.06, fontSize:6.8, color:"3A4A58", valign:"middle", fontFace:"Calibri", margin:0 });
  // Residual risk pill
  s4.addShape(pres.shapes.RECTANGLE, { x:10.64, y:ry+0.18, w:1.00, h:0.28, fill:{ color: r.resColor }, line:{ color: r.resColor } });
  s4.addText(r.res, { x:10.64, y:ry+0.18, w:1.00, h:0.28, fontSize:7, bold:true, color:"FFFFFF", align:"center", valign:"middle", fontFace:"Calibri", margin:0 });
  // Owner
  s4.addText(r.owner, { x:11.86, y:ry+0.04, w:1.08, h:rowH4-0.08, fontSize:7, color:"3A4A58", valign:"middle", fontFace:"Calibri", margin:0 });
});

s4.addText("INTERNAL — CONFIDENTIAL", { x:0.3, y:7.22, w:12.7, h:0.18, fontSize:7, color:C.light, align:"center", fontFace:"Calibri", margin:0 });

// ═══════════════════════════════════════════════════════════════════════════════
// SLIDE 5 — Architecture Diagram  (unchanged from previous version)
// ═══════════════════════════════════════════════════════════════════════════════
let s5 = pres.addSlide();
s5.background = { color: C.bg };
titleBar(s5, "Solution Architecture — GCP", "Security Design Authority  |  April 2026");

// ── EXTERNAL SYSTEMS STRIP ──
s5.addShape(pres.shapes.RECTANGLE, { x:0.05, y:0.42, w:1.25, h:6.85, fill:{ color: C.strip }, line:{ color: C.stripBdr, width:1 } });
s5.addText("External\nSystems", { x:0.05, y:0.42, w:1.25, h:0.36, fontSize:7, bold:true, color:"4A5560", align:"center", valign:"middle", fontFace:"Calibri", margin:0 });
function extBox(slide, label, fillColor, y) {
  slide.addShape(pres.shapes.RECTANGLE, { x:0.12, y, w:1.11, h:0.40, fill:{ color: fillColor }, line:{ color:"FFFFFF", width:1 } });
  slide.addText(label, { x:0.12, y, w:1.11, h:0.40, fontSize:6.5, bold:true, color:"FFFFFF", align:"center", valign:"middle", fontFace:"Calibri", margin:0 });
}
extBox(s5, "EIS Gateway",       "496E96", 0.86);
extBox(s5, "InvestiFi Gateway", "496E96", 1.34);
extBox(s5, "KYC-NCA",           "9A6A30", 2.00);
extBox(s5, "NLS / NRTS",        "9A6A30", 2.48);
extBox(s5, "CRRE",              "9A6A30", 2.96);

// ── GCP PROJECT BORDER ──
s5.addShape(pres.shapes.RECTANGLE, { x:1.38, y:0.42, w:11.87, h:6.85, fill:{ color:"FAFBFC" }, line:{ color: C.gcpBdr, width:1.5 } });
s5.addText("GCP Project", { x:1.5, y:0.44, w:2.2, h:0.20, fontSize:7, bold:true, color: C.gcpBdr, fontFace:"Calibri", margin:0 });

// ── ROW 1: SECURITY EDGE ──
const secY=0.67, secH=0.50;
function secBox(slide,label,fill,x,w){ slide.addShape(pres.shapes.RECTANGLE,{x,y:secY,w,h:secH,fill:{color:fill},line:{color:"FFFFFF",width:1}}); slide.addText(label,{x,y:secY,w,h:secH,fontSize:7,bold:true,color:"F0E8E8",align:"center",valign:"middle",fontFace:"Calibri",margin:0}); }
secBox(s5,"Cloud Armor\n(WAF)",             C.sec1,1.48,1.90);
secBox(s5,"Global HTTPS\nLoad Balancer",    C.sec2,3.48,2.10);
secBox(s5,"Google-managed\nSSL Certificates",C.sec3,5.68,2.10);
function cicdBox(slide,label,x){ slide.addShape(pres.shapes.RECTANGLE,{x,y:secY,w:1.28,h:secH,fill:{color:C.cicd},line:{color:C.cicdBdr,width:1}}); slide.addText(label,{x,y:secY,w:1.28,h:secH,fontSize:7,bold:true,color:"F5EDD8",align:"center",valign:"middle",fontFace:"Calibri",margin:0}); }
cicdBox(s5,"Artifact\nRegistry",10.32);
cicdBox(s5,"Cloud\nBuild",11.72);

// ── ROW 2: VPC ──
const vpcY=1.25, vpcH=2.68;
s5.addShape(pres.shapes.RECTANGLE,{x:1.48,y:vpcY,w:8.30,h:vpcH,fill:{color:C.computeBg},line:{color:C.gcpBdr,width:1.2}});
s5.addText("VPC — kyc-vpc",{x:1.56,y:vpcY+0.03,w:3,h:0.20,fontSize:7,bold:true,color:"4A6E90",fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.RECTANGLE,{x:1.58,y:vpcY+0.27,w:5.38,h:2.22,fill:{color:"DBE8F5"},line:{color:"5578A0",width:1}});
s5.addText("GKE Autopilot Cluster",{x:1.65,y:vpcY+0.30,w:5.18,h:0.20,fontSize:7,bold:true,color:C.computeD,fontFace:"Calibri",margin:0});
const gkeR1Y=vpcY+0.57, gkeW=1.58, gkeH=0.44;
function gkeBox(slide,label,x,y,hi){
  const f=hi?C.computeD:C.compute, b=hi?"33507A":C.computeD, bw=hi?1.5:1;
  slide.addShape(pres.shapes.RECTANGLE,{x,y,w:gkeW,h:gkeH,fill:{color:f},line:{color:b,width:bw}});
  slide.addText(label,{x,y,w:gkeW,h:gkeH,fontSize:6.5,bold:true,color:"EAF0F8",align:"center",valign:"middle",fontFace:"Calibri",margin:0});
}
gkeBox(s5,"API Gateway",           1.68,gkeR1Y);
gkeBox(s5,"Eureka\n(Service Reg.)",3.34,gkeR1Y);
gkeBox(s5,"Keycloak\n(OAuth 2.0)", 5.00,gkeR1Y);
const gkeR2Y=gkeR1Y+0.55;
gkeBox(s5,"kyc-orchestration ✦",  1.68,gkeR2Y,true);
gkeBox(s5,"screening-service",     3.34,gkeR2Y);
gkeBox(s5,"risk-service",          5.00,gkeR2Y);
gkeBox(s5,"viewer",                6.66,gkeR2Y);
s5.addText("HPA: min 2 → max 10 pods  |  PodDisruptionBudget",{x:1.65,y:gkeR2Y+0.50,w:5.18,h:0.20,fontSize:6,color:"5578A0",align:"center",italic:true,fontFace:"Calibri",margin:0});
// Data Layer
s5.addShape(pres.shapes.RECTANGLE,{x:7.10,y:vpcY+0.27,w:2.56,h:2.22,fill:{color:"ECEDF2"},line:{color:C.stripBdr,width:1}});
s5.addText("Data Layer",{x:7.16,y:vpcY+0.30,w:2.40,h:0.20,fontSize:7,bold:true,color:"4A5560",fontFace:"Calibri",margin:0});
function dataBox(slide,label,sub,fillC,bdrC,y,h){
  slide.addShape(pres.shapes.RECTANGLE,{x:7.18,y,w:2.36,h,fill:{color:fillC},line:{color:bdrC,width:1}});
  slide.addText(label,{x:7.18,y,w:2.36,h:h*0.55,fontSize:6.5,bold:true,color:"F0EEF5",align:"center",valign:"bottom",fontFace:"Calibri",margin:0});
  slide.addText(sub,  {x:7.18,y:y+h*0.55,w:2.36,h:h*0.45,fontSize:5.5,color:"D8D4E6",align:"center",valign:"top",fontFace:"Calibri",margin:0});
}
dataBox(s5,"Cloud SQL\nPostgreSQL 15 (HA)","CMEK · Private IP · Read Replica",C.db,   C.dbBdr,   vpcY+0.54,0.56);
dataBox(s5,"Memorystore Redis",            "Idempotency · Token cache",        C.cache,C.cacheBdr,vpcY+1.16,0.50);
dataBox(s5,"Cloud Storage",               "NLS Feedback · Audit Exports",     C.stor, C.storBdr, vpcY+1.74,0.48);
// Secrets & Identity
const sidX=9.88, sidW=3.30;
s5.addShape(pres.shapes.RECTANGLE,{x:sidX,y:vpcY,w:sidW,h:vpcH,fill:{color:C.idFill},line:{color:C.idBdr,width:1.2}});
s5.addText("Secrets & Identity",{x:sidX+0.07,y:vpcY+0.03,w:sidW-0.1,h:0.20,fontSize:7,bold:true,color:C.idBdr,fontFace:"Calibri",margin:0});
function identBox(slide,label,sub,y,h){
  slide.addShape(pres.shapes.RECTANGLE,{x:sidX+0.10,y,w:sidW-0.20,h,fill:{color:C.idBox},line:{color:C.idBoxD,width:1}});
  slide.addText(label,{x:sidX+0.10,y,w:sidW-0.20,h:h*0.52,fontSize:7,bold:true,color:"E0EEF2",align:"center",valign:"bottom",fontFace:"Calibri",margin:0});
  if(sub) slide.addText(sub,{x:sidX+0.10,y:y+h*0.52,w:sidW-0.20,h:h*0.48,fontSize:5.5,color:"A8CEDA",align:"center",valign:"top",fontFace:"Calibri",margin:0});
}
identBox(s5,"Secret Manager",      "HASHIDS_SALT · AES key\nOAuth secrets · API keys",vpcY+0.28,0.54);
identBox(s5,"Cloud KMS",           "CMEK for Cloud SQL & Storage",                    vpcY+0.89,0.48);
identBox(s5,"Workload Identity",   "No static JSON keys in pods",                     vpcY+1.44,0.48);
identBox(s5,"VPC Service Controls","Data exfiltration perimeter",                     vpcY+1.99,0.48);

// ── ROW 3: ASYNC ──
const asyncY=4.00, asyncH=0.84;
s5.addShape(pres.shapes.RECTANGLE,{x:1.48,y:asyncY,w:11.69,h:asyncH,fill:{color:C.asyncFill},line:{color:C.asyncBdr,width:1}});
s5.addText("Async & Scheduling",{x:1.56,y:asyncY+0.04,w:4,h:0.20,fontSize:7,bold:true,color:C.asyncBdr,fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.RECTANGLE,{x:1.58,y:asyncY+0.27,w:6.38,h:0.49,fill:{color:C.pubsub},line:{color:"8A6028",width:1}});
s5.addText("Cloud Pub/Sub",{x:1.58,y:asyncY+0.27,w:2.0,h:0.49,fontSize:7,bold:true,color:"F0E8D8",align:"center",valign:"middle",fontFace:"Calibri",margin:0});
s5.addText("kyc-webhook-delivery  |  kyc-webhook-deadletter  |  kyc-nls-feedback  |  kyc-case-poll-trigger  |  kyc-audit-events",{x:3.68,y:asyncY+0.27,w:4.28,h:0.49,fontSize:5.5,color:"E8D8B8",valign:"middle",fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.RECTANGLE,{x:8.08,y:asyncY+0.27,w:4.90,h:0.49,fill:{color:C.sched},line:{color:C.asyncBdr,width:1}});
s5.addText("Cloud Scheduler",{x:8.08,y:asyncY+0.27,w:2.0,h:0.49,fontSize:7,bold:true,color:"F0E8D8",align:"center",valign:"middle",fontFace:"Calibri",margin:0});
s5.addText("NLS poll (*/5 min)  |  Case finalization poll  |  SLA breach check (hourly)",{x:10.13,y:asyncY+0.27,w:2.82,h:0.49,fontSize:5.5,color:"E8D8B8",valign:"middle",fontFace:"Calibri",margin:0});

// ── ROW 4: OBSERVABILITY ──
const obsY=4.92, obsH=0.76, obsW=2.20;
s5.addShape(pres.shapes.RECTANGLE,{x:1.48,y:obsY,w:11.69,h:obsH,fill:{color:C.obsFill},line:{color:C.obsBdr,width:1}});
s5.addText("Observability",{x:1.56,y:obsY+0.04,w:3,h:0.20,fontSize:7,bold:true,color:C.obsBdr,fontFace:"Calibri",margin:0});
function obsBox(slide,label,sub,x){
  slide.addShape(pres.shapes.RECTANGLE,{x,y:obsY+0.25,w:obsW,h:0.44,fill:{color:C.obsBox},line:{color:C.obsBoxD,width:1}});
  slide.addText(label,{x,y:obsY+0.25,w:obsW,h:0.23,fontSize:6.5,bold:true,color:"E0EEE2",align:"center",valign:"middle",fontFace:"Calibri",margin:0});
  slide.addText(sub,  {x,y:obsY+0.47,w:obsW,h:0.22,fontSize:5.5,color:"A8C8AC",align:"center",valign:"top",fontFace:"Calibri",margin:0});
}
obsBox(s5,"Cloud Monitoring","Metrics · Alerting policies",  1.58);
obsBox(s5,"Cloud Trace",     "traceId · End-to-end tracing", 3.88);
obsBox(s5,"Cloud Logging",   "PII-masked structured logs",   6.18);
obsBox(s5,"Error Reporting", "PagerDuty integration",         8.48);
obsBox(s5,"BigQuery",        "Immutable audit · Compliance",  10.78);

// Security note + legend
s5.addShape(pres.shapes.RECTANGLE,{x:1.48,y:5.75,w:8.30,h:0.36,fill:{color:"F2EEEC"},line:{color:"8A5050",width:1}});
s5.addText("All PII encrypted AES-128/GCM at rest  |  TLS 1.2+ in transit  |  OAuth 2.0 Client Credentials  |  No public IPs on data services",{x:1.55,y:5.75,w:8.18,h:0.36,fontSize:6.5,color:"6A3A3A",valign:"middle",fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.RECTANGLE,{x:9.88,y:5.75,w:3.35,h:0.92,fill:{color:"F4F5F7"},line:{color:C.stripBdr,width:1}});
s5.addText("Legend",{x:9.95,y:5.77,w:3.20,h:0.20,fontSize:6.5,bold:true,color:"444E58",fontFace:"Calibri",margin:0});
[{color:C.compute,label:"Compute"},{color:C.db,label:"Database"},{color:C.pubsub,label:"Messaging"},{color:C.sec1,label:"Security"},{color:C.obsBox,label:"Observability"},{color:C.cicd,label:"CI/CD"},{color:C.idBox,label:"Identity"}].forEach((item,i)=>{
  const col=i<4?0:1, row=i<4?i:i-4, lx=9.95+col*1.65, ly=6.00+row*0.185;
  s5.addShape(pres.shapes.RECTANGLE,{x:lx,y:ly,w:0.11,h:0.11,fill:{color:item.color},line:{color:item.color}});
  s5.addText(item.label,{x:lx+0.15,y:ly,w:1.42,h:0.13,fontSize:5.8,color:"3A4450",valign:"middle",fontFace:"Calibri",margin:0});
});
// Arrows
s5.addShape(pres.shapes.LINE,{x:1.23,y:1.06,w:0.25,h:0,line:{color:C.arrBlue,width:1.0}});
s5.addShape(pres.shapes.LINE,{x:1.23,y:1.54,w:0.25,h:0,line:{color:C.arrBlue,width:1.0}});
s5.addShape(pres.shapes.LINE,{x:1.48,y:1.06,w:0,h:0.48,line:{color:C.arrBlue,width:1.0}});
s5.addText("HTTPS/TLS 1.2+",{x:1.50,y:0.87,w:1.1,h:0.16,fontSize:5,color:C.arrBlue,fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.LINE,{x:4.53,y:secY+secH,w:0,h:vpcY+0.79-(secY+secH),line:{color:C.arrBlue,width:1.0}});
s5.addShape(pres.shapes.LINE,{x:2.47,y:vpcY+0.79,w:2.06,h:0,line:{color:C.arrBlue,width:1.0}});
s5.addShape(pres.shapes.LINE,{x:3.26,y:gkeR2Y+0.22,w:3.92,h:0,line:{color:C.arrPurple,width:0.8,dashType:"sysDash"}});
s5.addText("Auth Proxy",{x:5.00,y:gkeR2Y+0.04,w:1.3,h:0.16,fontSize:5,color:C.arrPurple,fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.LINE,{x:1.23,y:2.20,w:0.45,h:0,line:{color:C.arrOrange,width:0.8,dashType:"sysDash"}});
s5.addShape(pres.shapes.LINE,{x:1.23,y:2.68,w:0.45,h:0,line:{color:C.arrOrange,width:0.8,dashType:"sysDash"}});
s5.addShape(pres.shapes.LINE,{x:1.23,y:3.16,w:0.45,h:0,line:{color:C.arrOrange,width:0.8,dashType:"sysDash"}});
s5.addText("REST/TLS",{x:0.08,y:2.52,w:1.12,h:0.16,fontSize:5,color:C.arrOrange,align:"center",fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.LINE,{x:2.47,y:gkeR2Y+gkeH,w:0,h:asyncY+0.52-(gkeR2Y+gkeH),line:{color:C.asyncBdr,width:0.8,dashType:"sysDash"}});
s5.addText("webhook events",{x:1.70,y:3.76,w:1.5,h:0.16,fontSize:5,color:C.asyncBdr,fontFace:"Calibri",margin:0});
s5.addShape(pres.shapes.LINE,{x:4.00,y:vpcY+vpcH,w:0,h:asyncY-(vpcY+vpcH),line:{color:C.arrGreen,width:0.8,dashType:"lgDash"}});
s5.addShape(pres.shapes.LINE,{x:4.00,y:asyncY+asyncH,w:0,h:obsY-(asyncY+asyncH),line:{color:C.arrGreen,width:0.8,dashType:"lgDash"}});

// ═══════════════════════════════════════════════════════════════════════════════
// SLIDE 6 — Component Reference
// ═══════════════════════════════════════════════════════════════════════════════
let s6 = pres.addSlide();
s6.background = { color: C.bg };
titleBar(s6, "Component Reference", "Security Design Authority  |  April 2026");

const groups = [
  { label:"Security Edge",        color:C.sec1,    textColor:"F0E8E8", items:[
    { name:"Cloud Armor (WAF)",              desc:"Layer-7 DDoS protection and OWASP rule-set; blocks malicious traffic before it reaches the load balancer." },
    { name:"Global HTTPS Load Balancer",     desc:"Anycast-based HTTPS LB; routes consumer and internal traffic to the nearest GKE node pool with health checks." },
    { name:"Google-managed SSL Certs",       desc:"Automatically provisioned and rotated TLS certificates; enforces TLS 1.2+ on all inbound connections." },
  ]},
  { label:"GKE Autopilot Cluster", color:C.compute, textColor:"EAF0F8", items:[
    { name:"API Gateway",                    desc:"Single ingress point; enforces OAuth 2.0 token validation, rate limiting, and request routing to downstream services." },
    { name:"Eureka (Service Registry)",      desc:"Netflix Eureka server; all microservices self-register so load-balanced RestTemplate calls resolve by name." },
    { name:"Keycloak (OAuth 2.0)",           desc:"Identity provider issuing JWT access tokens via Client Credentials flow for service-to-service authentication." },
    { name:"kyc-orchestration ✦",            desc:"Core service: orchestrates precheck, screening, risk and viewer calls; manages the full KYC lifecycle and webhook delivery." },
    { name:"screening-service",              desc:"Calls NCA screening API; returns Hit/No-Hit verdict used to set ON_HOLD status when a watchlist match is found." },
    { name:"risk-service",                   desc:"Evaluates applicant risk profile; returns LOW/MEDIUM/HIGH rating — HIGH triggers the ON_HOLD workflow." },
    { name:"viewer",                         desc:"Retrieves applicant identity data from the upstream document-check provider for precheck enrichment." },
  ]},
  { label:"Data Layer",            color:C.db,      textColor:"F0EEF5", items:[
    { name:"Cloud SQL PostgreSQL 15 (HA)",   desc:"Primary relational store for KYC audit records; CMEK-encrypted, Private IP only, with a read replica for reporting." },
    { name:"Memorystore Redis",              desc:"In-memory store for idempotency keys (preventing duplicate submissions) and OAuth token caching." },
    { name:"Cloud Storage",                  desc:"Object store for NLS polling feedback files and compliance audit exports forwarded to BigQuery." },
  ]},
  { label:"Secrets & Identity",    color:C.idBox,   textColor:"E0EEF2", items:[
    { name:"Secret Manager",                 desc:"Centralised secret store for HASHIDS_SALT, AES-128 encryption key, OAuth client secrets, and third-party API keys." },
    { name:"Cloud KMS",                      desc:"Manages Customer-Managed Encryption Keys (CMEK) used by Cloud SQL and Cloud Storage to satisfy data residency requirements." },
    { name:"Workload Identity",              desc:"Binds GKE service accounts to GCP IAM roles; pods acquire credentials at runtime — no static JSON key files." },
    { name:"VPC Service Controls",           desc:"Access perimeter around Cloud SQL, Secret Manager, and Storage; prevents data exfiltration even if a pod is compromised." },
  ]},
  { label:"Async & Scheduling",    color:C.pubsub,  textColor:"F0E8D8", items:[
    { name:"Cloud Pub/Sub",                  desc:"Async message bus; topics cover webhook delivery (with dead-letter), NLS feedback, case polling triggers, and audit events." },
    { name:"Cloud Scheduler",                desc:"Cron triggers for NLS polling (every 5 min), case-finalisation checks, and hourly SLA-breach alerting." },
  ]},
  { label:"Observability",         color:C.obsBox,  textColor:"E0EEE2", items:[
    { name:"Cloud Monitoring",               desc:"Collects pod metrics (CPU, heap, HTTP latency p99); feeds alerting policies that notify on-call via PagerDuty." },
    { name:"Cloud Trace",                    desc:"Distributed tracing with propagated traceId; visualises end-to-end latency across all microservice hops." },
    { name:"Cloud Logging",                  desc:"Structured JSON logs with PII masked before emission; queryable via Log Explorer and exportable to BigQuery." },
    { name:"Error Reporting",                desc:"Automatically groups and deduplicates exceptions; triggers PagerDuty incidents for new error classes." },
    { name:"BigQuery",                       desc:"Immutable audit trail of all KYC events; powers compliance exports and long-term retention (7 years)." },
  ]},
  { label:"CI/CD",                 color:C.cicd,    textColor:"F5EDD8", items:[
    { name:"Artifact Registry",              desc:"Private Docker registry; stores signed container images and build artefacts per environment." },
    { name:"Cloud Build",                    desc:"Serverless CI pipeline: runs tests, builds images, signs with Binary Authorization, and deploys to GKE via Helm." },
  ]},
  { label:"External Systems",      color:"496E96",  textColor:"EAF0F8", items:[
    { name:"EIS Gateway",                    desc:"Enterprise Integration Service; primary caller initiating KYC precheck on behalf of front-office systems." },
    { name:"InvestiFi Gateway",              desc:"Investment platform gateway; triggers KYC for new investment account onboarding journeys." },
    { name:"KYC-NCA",                        desc:"National competent authority screening endpoint; provides hit/no-hit response for watchlist checks." },
    { name:"NLS / NRTS",                     desc:"Name/Reference Translation Services; polled periodically for case status updates and NLS feedback files." },
    { name:"CRRE",                           desc:"Compliance Risk and Reporting Engine; receives enriched KYC outcomes for downstream regulatory reporting." },
  ]},
];

const COL_X=[0.15,4.52,8.89], COL_W=4.25, START_Y=0.50, HDR_H=0.26, ROW_H=0.52, GAP=0.10;
const cols6=[[[],0.50],[[],0.50],[[],0.50]];
let ci6=0;
for(const g of groups){
  const gh=HDR_H+g.items.length*ROW_H+GAP;
  cols6[ci6][0].push(g);
  cols6[ci6][1]+=gh;
  if(cols6[ci6][1]>7.30 && ci6<2) ci6++;
}
cols6.forEach(([col],ci)=>{
  let curY=START_Y;
  col.forEach(group=>{
    s6.addShape(pres.shapes.RECTANGLE,{x:COL_X[ci],y:curY,w:COL_W,h:HDR_H,fill:{color:group.color},line:{color:group.color}});
    s6.addText(group.label,{x:COL_X[ci]+0.08,y:curY,w:COL_W-0.12,h:HDR_H,fontSize:7.5,bold:true,color:group.textColor,valign:"middle",fontFace:"Calibri",margin:0});
    curY+=HDR_H;
    group.items.forEach((item,idx)=>{
      const rf=idx%2===0?"FFFFFF":"F0F2F4";
      s6.addShape(pres.shapes.RECTANGLE,{x:COL_X[ci],y:curY,w:COL_W,h:ROW_H,fill:{color:rf},line:{color:"D8DADE",width:0.5}});
      s6.addShape(pres.shapes.RECTANGLE,{x:COL_X[ci],y:curY,w:0.04,h:ROW_H,fill:{color:group.color},line:{color:group.color}});
      s6.addText(item.name,{x:COL_X[ci]+0.10,y:curY+0.03,w:COL_W-0.14,h:0.18,fontSize:7,bold:true,color:"2C3E50",fontFace:"Calibri",margin:0});
      s6.addText(item.desc,{x:COL_X[ci]+0.10,y:curY+0.20,w:COL_W-0.14,h:ROW_H-0.22,fontSize:6,color:"4A5560",fontFace:"Calibri",margin:0});
      curY+=ROW_H;
    });
    curY+=GAP;
  });
});

// ═══════════════════════════════════════════════════════════════════════════════
// SLIDE 7 — Decision Required & Next Steps
// ═══════════════════════════════════════════════════════════════════════════════
let s7 = pres.addSlide();
s7.background = { color: C.bg };
titleBar(s7, "Decision Required & Next Steps", "Security Design Authority  |  April 2026");

// Decision box
s7.addShape(pres.shapes.RECTANGLE, { x:0.30, y:0.55, w:12.70, h:1.00, fill:{ color:"EBF0F5" }, line:{ color: C.gcpBdr, width:1.5 } });
s7.addShape(pres.shapes.RECTANGLE, { x:0.30, y:0.55, w:0.06,  h:1.00, fill:{ color: C.compute }, line:{ color: C.compute } });
s7.addText("Decision Required", { x:0.44, y:0.58, w:3.5, h:0.30, fontSize:10, bold:true, color:"2C3E50", fontFace:"Calibri", margin:0 });
s7.addText("The Security Design Authority is asked to approve the proposed GCP architecture for the KYC Orchestration Service, confirming that the security controls, data handling practices and deployment model satisfy the firm's information security and regulatory requirements.", {
  x:0.44, y:0.88, w:12.50, h:0.56, fontSize:8.5, color:"2C3E50", fontFace:"Calibri", margin:0
});

// Conditions / assumptions
s7.addText("Conditions & Assumptions", { x:0.30, y:1.68, w:6.0, h:0.26, fontSize:9.5, bold:true, color:"2C3E50", fontFace:"Calibri", margin:0 });
const conditions = [
  "Penetration test to be completed prior to production go-live; findings remediated to at least Medium severity.",
  "HASHIDS_SALT and AES-128 key to be rotated from default values before any environment above Development.",
  "VPC Service Controls perimeter to be validated by the Security team during the Infrastructure provisioning phase.",
  "Production Cloud SQL instance to be configured with deletion protection and point-in-time recovery enabled.",
];
conditions.forEach((c, i) => {
  s7.addShape(pres.shapes.OVAL, { x:0.38, y:1.98+i*0.34, w:0.16, h:0.16, fill:{ color: C.compute }, line:{ color: C.compute } });
  s7.addText(String(i+1), { x:0.38, y:1.98+i*0.34, w:0.16, h:0.16, fontSize:6.5, bold:true, color:"FFFFFF", align:"center", valign:"middle", fontFace:"Calibri", margin:0 });
  s7.addText(c, { x:0.62, y:1.95+i*0.34, w:5.90, h:0.32, fontSize:8, color:"2C3E50", valign:"middle", fontFace:"Calibri", margin:0 });
});

// Divider
s7.addShape(pres.shapes.LINE, { x:6.90, y:1.68, w:0, h:3.80, line:{ color:"D0D8E0", width:1 } });

// Delivery phases (right half)
s7.addText("Indicative Delivery Plan", { x:7.10, y:1.68, w:6.0, h:0.26, fontSize:9.5, bold:true, color:"2C3E50", fontFace:"Calibri", margin:0 });

const phases = [
  { phase:"Phase 1", label:"Infrastructure Provisioning",   weeks:"Weeks 1–4",  color: C.idBox,   items:["GKE Autopilot cluster, VPC and subnets","Cloud SQL HA instance and Memorystore Redis","Secret Manager, KMS keys, Workload Identity bindings","Cloud Armor policy and SSL certificates"] },
  { phase:"Phase 2", label:"Service Development & Testing", weeks:"Weeks 5–12", color: C.compute, items:["kyc-orchestration service build and unit tests","Integration with screening, risk, viewer and external partners","Pub/Sub topics, Cloud Scheduler jobs, webhook delivery","CI/CD pipeline (Cloud Build + Artifact Registry)"] },
  { phase:"Phase 3", label:"UAT & Security Testing",        weeks:"Weeks 13–16",color: C.stor,    items:["User acceptance testing with EIS and InvestiFi teams","Penetration test and vulnerability remediation","Performance and load testing (target: p99 < 2 s)","SDA pre-production sign-off checkpoint"] },
  { phase:"Phase 4", label:"Production Deployment",         weeks:"Weeks 17–18",color: C.cicd,    items:["Blue-green deployment to production GKE cluster","Runbook and operational handover to Platform team","Hypercare period: 2 weeks with engineering on-call"] },
];

phases.forEach((p, i) => {
  const py = 2.00 + i * 1.22;
  // Phase bar
  s7.addShape(pres.shapes.RECTANGLE, { x:7.10, y:py, w:5.90, h:0.28, fill:{ color: p.color }, line:{ color: p.color } });
  s7.addText(`${p.phase}  ·  ${p.label}`, { x:7.18, y:py, w:4.20, h:0.28, fontSize:8, bold:true, color:"FFFFFF", valign:"middle", fontFace:"Calibri", margin:0 });
  s7.addText(p.weeks, { x:11.30, y:py, w:1.64, h:0.28, fontSize:7.5, color:"F0F4F8", align:"right", valign:"middle", fontFace:"Calibri", margin:0 });
  // Items
  s7.addShape(pres.shapes.RECTANGLE, { x:7.10, y:py+0.28, w:5.90, h:0.90, fill:{ color:"FFFFFF" }, line:{ color:"D0D8E0", width:0.5 } });
  p.items.forEach((it, j) => {
    s7.addShape(pres.shapes.RECTANGLE, { x:7.18, y:py+0.35+j*0.20, w:0.06, h:0.06, fill:{ color: p.color }, line:{ color: p.color } });
    s7.addText(it, { x:7.30, y:py+0.30+j*0.20, w:5.62, h:0.20, fontSize:7, color:"3A4A58", valign:"middle", fontFace:"Calibri", margin:0 });
  });
});

// Contacts footer
s7.addShape(pres.shapes.RECTANGLE, { x:0.30, y:5.60, w:12.70, h:0.50, fill:{ color:"EBF0F5" }, line:{ color:"B0BEC8", width:1 } });
s7.addText("Document Owner: KYC Engineering  |  Sponsor: Chief Architect  |  Classification: Internal — Confidential  |  Review Date: April 2026", {
  x:0.44, y:5.60, w:12.44, h:0.50, fontSize:8, color:"3A4A58", valign:"middle", align:"center", fontFace:"Calibri", margin:0
});

// ─── Write file ───────────────────────────────────────────────────────────────
pres.writeFile({ fileName: "/Users/venusamineni/Documents/Claude/Projects/kyc-react-ms/KYC_GCP_Architecture.pptx" })
  .then(() => console.log("✅  Saved: KYC_GCP_Architecture.pptx"))
  .catch(err => { console.error("❌ Error:", err); process.exit(1); });
