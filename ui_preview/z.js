(()=>{
'use strict';
const $=s=>document.querySelector(s),$$=s=>Array.from(document.querySelectorAll(s));
const pad=n=>String(n).padStart(2,'0');
const hh=min=>pad(Math.floor(((min%1440)+1440)%1440/60))+':'+pad(((min%1440)+1440)%1440%60);
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
const esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
const seedRand=function(s){return function(){s=(s*1664525+1013904223)>>>0;return s/4294967296;};};
const CHCATS=['news','news','news','news','news','show','doku','serie','doku','doku','news','doku','news','news','news','news','news','sport','film','sport','sport','doku','news','news','kids','kids'];
const LIVE=[
['Das Erste','DAS','#24437a','#3e6cb5','Tagesschau',1],
['ZDF','ZDF','#001d45','#335d9c','heute journal',2],
['RTL','RTL','#2a6ae0','#18a0fb','Nachtjournal',3],
['Sat.1','SAT1','#2a2a3d','#4c4c6e','Sat.1 Nachrichten',4],
['ProSieben','P7','#e33e5e','#7d2a52','taff',5],
['VOX','VOX','#e8a800','#7a5200','Das perfekte Dinner',6],
['Kabel Eins','K1','#d92d27','#7a1a17','Kabel Eins News',7],
['RTLZWEI','R2','#7a2a9c','#c04dff','Berlin Tag & Nacht',8],
['ZDFneo','NEO','#101426','#3a4a8c','neo xplorer',9],
['ARTE','ARTE','#2a7b2f','#4caf50','ARTE Reportage',10],
['one','1','#0e2b52','#1e5aa8','one-News',11],
['3sat','3SAT','#0b3b48','#159ba8','nano',12],
['WDR','WDR','#1c4d8c','#3f7fd6','Aktuelle Stunde',13],
['NDR','NDR','#0c4d3f','#148a6b','NDR Info',14],
['SWR','SWR','#7a0c0c','#c41c1c','SWR Aktuell',15],
['MDR','MDR','#0a3d6b','#1274b8','MDR Aktuell',16],
['Sky','SKY','#0b1c3a','#1e4b9c','Sky News',17],
['Sky Sport','S-S','#083344','#0d7d95','Fußball: Bundesliga',18],
['Sky Cinema','CIN','#3a0c5e','#7a2ec4','Action Now',19],
['Sport1','SP1','#243645','#4a7a8c','Sport1 News',20],
['Eurosport','EU','#cf3c3c','#6b1e1e','Tennis: Grand Slam',21],
['DMAX','MAX','#0b2418','#2a6b45','Steel Buddies',22],
['n-tv','NTV','#1c2a6e','#2e4bd6','n-tv Nachrichten',23],
['WELT','WELT','#111722','#3b4a63','WELT Nachrichten',24],
['Nickelodeon','NICK','#d97b00','#f5a623','SpongeBob Schwammkopf',25],
['Disney Channel','DIS','#12317a','#2c58c4','Phineas und Ferb',26],
];
const POOL=[
['Tagesschau','news'],['heute','news'],['heute journal','news'],['WELT Nachrichten','news'],['n-tv Nachrichten','news'],['Tagesthemen','news'],['zdf morgenmagazin','news'],['Sat.1 Frühstücksfernsehen','news'],['Börse im Blick','news'],['Wetter aktuell','news'],['Sport1 News','news'],['Sky News','news'],
['Tatort','serie'],['Polizeiruf 110','serie'],['Mord mit Aussicht','serie'],['Wilsberg','serie'],['Nord bei Nordwest','serie'],['Die Chefin','serie'],['SOKO Leipzig','serie'],['SOKO Köln','serie'],['Notruf Hafenkante','serie'],['Großstadtrevier','serie'],['The Big Bang Theory','serie'],['Two and a Half Men','serie'],['NCIS','serie'],['Criminal Minds','serie'],['CSI: Den Tätern auf der Spur','serie'],["Grey's Anatomy",'serie'],['The Walking Dead','serie'],['Raumschiff Enterprise','serie'],['Star Trek: Picard','serie'],['Bergretter','serie'],['Der Staatsanwalt','serie'],
['Die Doku: Dom-Rebellen','doku'],['Doku: Planet Erde','doku'],['Doku: Unsere Ozeane','doku'],['Spiegel TV','doku'],['hart aber fair','doku'],['Anne Will','doku'],['ZDF Magazin Royale','doku'],['Die Anstalt','doku'],['Extra 3','doku'],['Brandpunkt','doku'],['Mythen & Monster','doku'],["How It's Made",'doku'],['Storm Chasers','doku'],['Trödeltrupp','doku'],['Bares für Rares','show'],
['Das perfekte Dinner','show'],['First Dates – ein Date, das zählt','show'],['Mein Lokal, Dein Lokal','show'],['Hochzeit auf den ersten Blick','show'],['Shopping Queen','show'],['Der Blaulicht Report','show'],['Auf Streife','show'],['K11 – Die neuen Fälle','show'],['Ab ins Beet!','show'],['Nur die Liebe zählt','show'],
['ZDFzeit','doku'],['nano','news'],['arte Journal','news'],['Wissenschaft: Quantenwelten','doku'],['Star Wars: Die letzte Jedi – Docu','film'],['Top Gun: Maverick','film'],['Dune: Teil Zwei','film'],['Oppenheimer','film'],['Der Pate – Teil II','film'],['Mad Max: Fury Road','film'],['Inception','film'],['Interstellar','film'],['Ziemlich beste Freunde','film'],['Der Schuh des Manitu','film'],['UEFA Europa League','sport'],['Bundesliga: Topspiel','sport'],['Sky Sport Bundesliga Live','sport'],['Tennis: Grand Slam Halbfinale','sport'],['Boxen: Heavyweight Night','sport'],['Wrestling: Royal Rumble','sport'],
['SpongeBob Schwammkopf','kids'],['Phineas und Ferb','kids'],['Disneys Sofia','kids'],['Die Gummibärenbande','kids'],['Alvin und die Chipmunks','kids'],['Paw Patrol','kids'],['Bibi Blocksberg','kids'],['Pumuckl','kids'],
];
const POSTS=[
['Dune: Teil Zwei','NEU'],['Oppenheimer','NEU'],['Top Gun: Maverick','4K'],['Der Pate – Teil II','HD'],['Inception','4K'],['Interstellar','HDR'],['Mad Max: Fury Road','4K'],['Ziemlich beste Freunde','HD'],['Der Schuh des Manitu','HD'],['Star Wars: Erwachen der Macht','4K'],['Die Tribute von Panem','HD'],['Verrückt nach Mary','HD'],['Shutter Island','4K'],['Arrival','HDR'],['Gladiator','4K'],['Das Schweigen der Lämmer','HD'],
];
const MIX=[
['Zen FM – Chill','live'],['Zen FM – Vinyl Klassiker','radio'],['Urban Beats','radio'],['Klassik Café','radio'],['Rock-Arena','radio'],['Kindermeile','radio'],['Deep Focus','radio'],['Feierabend Mix','mix'],
];
const PAL=[
['#5b3df0','#c24bd8'],['#0fa3a3','#25d1a0'],['#e05d3c','#f7a53c'],['#2764d8','#57a7ff'],['#833ab4','#fd1d1d'],['#11998e','#38ef7d'],['#4b6cb7','#182848'],['#fe8c00','#f83600'],['#0f2027','#2c5364'],['#c31432','#240b23'],['#1a2980','#26d0ce'],['#7f00ff','#e100ff'],['#134e5e','#71b280'],['#fc466b','#3f5efb'],['#2c3e50','#4ca1af'],['#200122','#6f0000'],
];
const THEMES=[
{id:'zen',n:'Zen',a:'#35cfb2',b:'#3d7bff',bl:['#12424e','#16345c','#171040'],h:['#16345c','#12424e','#07070c'],bg:['#0d0d16','#07070c']},
{id:'sky',n:'Sky',a:'#7a3ff2',b:'#e0339c',bl:['#22124c','#4c1240','#12082a'],h:['#1e1044','#3f0f33','#0b0516'],bg:['#120d1a','#08050c']},
{id:'magenta',n:'Magenta',a:'#ff0099',b:'#6a11cb',bl:['#3d0f26','#24104a','#140624'],h:['#33091f','#1d0d3d','#0a0414'],bg:['#120a12','#080608']},
{id:'waipu',n:'Waipu',a:'#00b3ff',b:'#0057ff',bl:['#062c4c','#0a1c5c','#080826'],h:['#062c4c','#0a1c5c','#04081a'],bg:['#0a1018','#06080c']},
{id:'plex',n:'Plex',a:'#e5a00d',b:'#8a5a00',bl:['#3d2c06','#241a04','#120e04'],h:['#352708','#1f1805','#0e0a03'],bg:['#140f08','#0a0704']},
{id:'pluto',n:'Pluto',a:'#ffd400',b:'#e50914',bl:['#3d3006','#4c0c10','#241006'],h:['#352806','#400b0e','#120b03'],bg:['#14120a','#0a0804']},
{id:'netflix',n:'Netflix',a:'#e50914',b:'#7a0000',bl:['#3d090c','#24060a','#140508'],h:['#35080b','#200507','#0c0304'],bg:['#100808','#080404']},
{id:'disney',n:'Disney+',a:'#5b6ef5',b:'#0c3dd1',bl:['#101c4c','#0a1440','#080c2a'],h:['#0e183d','#0a1234','#050719'],bg:['#0a0e18','#06080c']},
{id:'prime',n:'Prime',a:'#00a8e1',b:'#1f7da0',bl:['#062f40','#0a2432','#08141c'],h:['#072837','#081f29','#040d12'],bg:['#0a1014','#05080c']},
{id:'youtube',n:'YouTube',a:'#ff0040',b:'#ff7a00',bl:['#40060f','#2e1604','#140608'],h:['#38050d','#2a1204','#100406'],bg:['#120a08','#090504']},
{id:'spotify',n:'Spotify',a:'#1db954',b:'#0d7a3a',bl:['#0a3d22','#0c2e1c','#060c08'],h:['#0c3320','#0a2418','#040a06'],bg:['#0a100c','#060806']},
{id:'apple',n:'Apple TV+',a:'#64d2ff',b:'#0a2351',bl:['#0a2a4c','#0a1632','#050a18'],h:['#0a2440','#081228','#040712'],bg:['#0a0f18','#05070c']},
{id:'heaven',n:'Himmel',a:'#7bd5ff',b:'#ff7be8',bl:['#0c3f63','#5c2556','#1c1130'],h:['#0e3a56','#5c2556','#0a0a10'],bg:['#0d0d1a','#07070f']},
{id:'friend',n:'Freund',a:'#ff6b6b',b:'#ffd93d',bl:['#5c1e1e','#6e5a12','#2a1010'],h:['#5c1e1e','#6e5a12','#120808'],bg:['#16100f','#0a0606']},
{id:'noir',n:'Noir',a:'#c9d2e3',b:'#6b7b98',bl:['#232a38','#141a26','#0c1018'],h:['#1c222e','#12161f','#07090d'],bg:['#0c0e13','#050608']},
{id:'glacier',n:'Gletscher',a:'#9ff0ff',b:'#4db6e8',bl:['#0e4757','#1b3a5e','#0c1826'],h:['#0e4757','#1b3a5e','#050d14'],bg:['#0b1016','#05080c']},
{id:'neon',n:'Neon',a:'#00ffb3',b:'#ff2d95',bl:['#06403a','#4c0f38','#0a0520'],h:['#06403a','#4c0f38','#040610'],bg:['#0c0f16','#05060a']},
{id:'mono',n:'Mono',a:'#e8e8ec',b:'#9aa0ab',bl:['#2e3138','#1c1e24','#0b0c10'],h:['#22242b','#16181e','#07080b'],bg:['#0d0d11','#060608']},
{id:'orchis',n:'Orchidee',a:'#d95cff',b:'#7a5cff',bl:['#421455','#24145c','#180a24'],h:['#421455','#24145c','#0c0412'],bg:['#120e18','#09070c']},
{id:'ember',n:'Glut',a:'#ff7a45',b:'#ff3d2d',bl:['#5c1e0c','#42100a','#1e0604'],h:['#5c1e0c','#42100a','#120302'],bg:['#150c0a','#0a0605']},
{id:'pine',n:'Tanne',a:'#6bff8a',b:'#25d1a0',bl:['#0e3d26','#0a3d34','#06160c'],h:['#0e3d26','#0a3d34','#040d07'],bg:['#0b120e','#060a07']},
{id:'ocean',n:'Ozean',a:'#4fd8ff',b:'#3d7bff',bl:['#0a2a4c','#0e1c4c','#080c20'],h:['#0a2a4c','#0e1c4c','#040718'],bg:['#0a0f18','#05070c']},
];
const DEF={theme:'zen',fx:'ring',glass:55,bg:'wave',animbg:1,font:1,density:2,nav:'rail',homet:'hub',ui:'grad',wblur:40,favs:[0,2,4,6],
epg:{logos:1,pipp:1,autonow:1,day:0,favsonly:0,size:1,colw:1},play:{eng:'exo',q:'4K',zinfo:1,autohide:4.2,ch:0},
prof:{nick:'Gast'},scale:{home:1,browse:1,epg:1,player:1,settings:1}};
let S=JSON.parse(JSON.stringify(DEF));
const ST={
load(){try{const s=localStorage.getItem('zp_v10');if(!s)return;const d=JSON.parse(s);Object.keys(DEF).forEach(k=>{if(d[k]!==undefined){if(k==='epg'||k==='play'||k==='prof'||k==='scale')Object.assign(S[k],d[k]);else if(k==='favs'&&Array.isArray(d[k]))S.favs=[...d[k]];else S[k]=d[k];}});}catch(e){}},
save(){try{localStorage.setItem('zp_v10',JSON.stringify(S));}catch(e){}},
};
const nowMin=()=>{const d=new Date();return d.getHours()*60+d.getMinutes();};
const dLabel=()=>{const d=new Date(Date.now()+S.epg.day*864e5);return d.toLocaleDateString('de-DE',{weekday:'short',day:'2-digit',month:'2-digit'});};
const logoHtml=(ch,cls)=>'<span class="logo '+(cls||'')+'" style="background:linear-gradient(135deg,'+ch[2]+','+ch[3]+')">'+esc(ch[1])+'</span>';
const art=ch=>'linear-gradient(128deg,'+ch[2]+', '+ch[3]+' 55%, #04040a)';
const postArt=i=>'linear-gradient(135deg,'+PAL[i%PAL.length][0]+','+PAL[i%PAL.length][1]+')';
const PROGS={};
function progsFor(i,day){
const k=i+'_'+day;if(PROGS[k])return PROGS[k];
const ch=LIVE[i];const rnd=seedRand(i*7919+day*104729+77);
const cat=CHCATS[i];const p=POOL.filter(x=>x[1]===cat||rnd()<.16);
const durs=[15,20,25,30,30,35,40,45,45,50,60,60,75,90,105,120,15,30,45];
const out=[];let m=300;const end=300+1440;
let last=Math.floor(rnd()*p.length);
while(m<end){let dr=durs[Math.floor(rnd()*durs.length)];if(m+dr>end)dr=end-m;if(dr<10){m+=dr;continue;}let pick=last;for(let k=0;k<8;k++){const j=Math.floor(rnd()*p.length);if(rnd()<.5)pick=j;}out.push({s:m,e:m+dr,t:p[pick%p.length][0],c:p[pick%p.length][1]});last=pick;m+=dr;}
PROGS[k]=out;return out;
}
function progAt(i,min){const a=progsFor(i,S.epg.day);let b=0;for(let k=0;k<a.length;k++)if(a[k].s<=min)b=k;return b;}
function nowProg(i){return progsFor(i,S.epg.day)[progAt(i,nowMin())];}
function themeById(id){return THEMES.find(t=>t.id===id)||THEMES[0];}
function applyTheme(){const t=themeById(S.theme),r=document.documentElement.style;
r.setProperty('--acc-a',t.a);r.setProperty('--acc-b',t.b);r.setProperty('--blob-a',t.bl[0]);r.setProperty('--blob-b',t.bl[1]);r.setProperty('--blob-c',t.bl[2]);
r.setProperty('--h1',t.h[0]);r.setProperty('--h2',t.h[1]);r.setProperty('--h3',t.h[2]);r.setProperty('--bg0',t.bg[1]);r.setProperty('--bg1',t.bg[0]);
r.setProperty('--pgnow',t.a+'22');}
function applyGlass(){const g=S.glass/100;const r=document.documentElement.style;
r.setProperty('--glass',Math.round(8+g*64)+'px');r.setProperty('--glassbg','rgba(10,10,18,'+(0.16+g*0.42).toFixed(3)+')');r.setProperty('--wblur',Math.round(6+S.wblur*0.35)+'px');}
function applyBg(){const b=document.body;
['bg-wave','bg-aurora','bg-grad','bg-holo','bg-plain'].forEach(c=>b.classList.remove(c));
b.classList.add('bg-'+S.bg);
b.classList.toggle('no-animbg',!S.animbg);
const aur=$('#auroraEl'),hol=$('#holoEl'),liq=$('#liquidBg');
if(aur&&hol){aur.style.display=(S.bg==='aurora')?'block':'none';hol.style.display=(S.bg==='holo')?'block':'none';liq.style.background=(S.bg==='grad')?'linear-gradient(165deg,var(--bg1),var(--blob-b),var(--bg0))':'';}}
function applyFont(){document.body.dataset.font=String(S.font);document.documentElement.style.setProperty('--zfs',[0.9,1,1.12][S.font]);}
function applyDensity(){const d=den();const r=document.documentElement.style;
r.setProperty('--eph',d.eph+'px');r.setProperty('--ech',d.ech+'px');r.setProperty('--etw',d.etw+'px');r.setProperty('--eps',d.eps*([0.9,1,1.1][S.font]||1));r.setProperty('--gh',Math.max(42,Math.round(d.eph*0.62))+'px');}
function applyFx(){document.body.dataset.fx=S.fx;}
function applyNav(){document.body.dataset.navv=S.nav;}
function applyUi(){document.body.dataset.ui=S.ui;}
function applyScale(){['#s-home','#s-browse','#s-epg','#s-player','#s-settings'].forEach(e=>$(e).style.setProperty('--sc','1'));['home','browse','epg','player','settings'].forEach((k,i)=>{$(['#s-home','#s-browse','#s-epg','#s-player','#s-settings'][i]).style.setProperty('--sc',S.scale[k]);});}
function applyAll(){applyTheme();applyGlass();applyBg();applyFont();applyDensity();applyFx();applyNav();applyUi();applyScale();}
function setCh(i){i=((i%LIVE.length)+LIVE.length)%LIVE.length;S.play.ch=i;updateChViews();}
function curCh(){return LIVE[S.play.ch];}
let clockT=null;
function updateChViews(){
const ch=curCh(),pr=nowProg(S.play.ch);
$('#pTitle').textContent=ch[0];$('#pSub').textContent=pr?pr.t:'–';
$('#pQ').textContent=(S.play.q||'4K')+' · '+(S.play.eng==='vlc'?'VLC':'ExoPlayer');
$('#zLogo').style.background='linear-gradient(135deg,'+ch[2]+','+ch[3]+')';$('#zLogo').textContent=ch[1];
$('#zNum').textContent=String(ch[5]);$('#zName').textContent=ch[0];$('#zProg').textContent=pr?pr.t:'–';
$('#pvidArt').style.background=art(ch);$('#pvidLg').textContent=ch[1];
if(pr){const same=nowMin();const pc=clamp((same-pr.s)/(pr.e-pr.s),0,1)*100;
$('#pTE').textContent=hh(pr.s);$('#pTR').textContent='-'+fmtDur(pr.e-same);$('#progBar .filled').style.width=pc+'%';$('#progBar .dot').style.left=pc+'%';
$('#pNow').textContent=pr.t;
const a=progsFor(S.play.ch,S.epg.day);const nx=a[progAt(S.play.ch,pr.e)];$('#pNext').textContent=(nx&&nx!==pr?' '+hh(nx.s)+' · '+nx.t:'–');
$('#pDesc').textContent=prDesc(S.play.ch,pr);}
updatePip();}
function prDesc(ci,p){const ch=LIVE[ci];if(!p)return 'Sendung auf '+ch[0]+' – Kanal '+ch[5]+'.';
const s=''+p.t;let g=({news:'Nachrichten',doku:'Dokumentation',serie:'Serie',show:'Show',sport:'Sport',film:'Film',kids:'Kindersendung'})[CHCATS[ci]]||'Sendung';
if(s.match(/frühst|morgen/i))g='Magazin';else if(s.match(/film|movie|shark|haie|winton/i))g='Spielfilm';else if(s.match(/feiertags|talk|show|magazin/i))g='Show';
return 'Jetzt auf '+ch[0]+' (Kanal '+ch[5]+'): „'+s+'“ – eine '+g+'-Sendung, begonnen um '+hh(p.s)+' Uhr.';}
function fmtDur(m){if(m<=0)m=0;const h=Math.floor(m/60),mm=m%60;return (h>0?h+':':'')+pad(mm);}
function updatePip(){
const ch=curCh();$('#pipBg').style.background=art(ch);$('#pipWm').textContent=ch[1];
$('#pipName').textContent=ch[0];$('#pipProg').textContent=nowProg(S.play.ch)?nowProg(S.play.ch).t:'–';
const on=S.screen==='epg'&&S.epg.pipp&&previewOn;
$('#pip').classList.toggle('show',on);}
function toast(m){const t=document.createElement('div');t.className='toast';t.textContent=m;document.body.appendChild(t);setTimeout(()=>t.remove(),2200);}
let previewOn=false;
const SCREENS={home:'s-home',live:'s-browse',vod:'s-browse',music:'s-browse',epg:'s-epg',settings:'s-settings'};
const NAVMETA=[['home','Start'],['epg','TV-Programm'],['vod','Videos'],['music','Musik'],['settings','Einstellungen']];
function openNav(n){
S.screen=n;S.nav=n;
$$('.screen').forEach(e=>e.classList.toggle('on',e.id===SCREENS[n]));
$$('[data-nav]').forEach(e=>e.classList.toggle('on',e.dataset.nav===n));
if(n==='home')buildHome();
else if(n==='live'||n==='vod'||n==='music')buildBrowse(n);
else if(n==='epg')buildEpg();
else if(n==='settings')buildSettings();
updatePip();
refreshNavPill();
}
function refreshNavPill(){
$$('.nitem,.titem').forEach(e=>e.classList.remove('nl'));
const cur=$$('.nitem[data-nav],.titem[data-nav]').find(e=>e.dataset.nav===S.screen&&S.screen!=='live'&&S.screen!=='vod'&&S.screen!=='music');
}
function hSection(t,all,html){return '<div class="sec"><h2>'+t+'</h2><span class="all">'+all+'</span></div>'+html;}
const heroHtml='<div class="hero" data-act="toast" data-t="Live-Übertragung startet"><div class="art"><i class="a1"></i><i class="a2"></i></div><div class="shade"></div><div class="shine"></div><div class="glowbox"><div class="meta"><span class="hd">LIVE</span><span>Sender 17 · Sky</span><span>Spielfilm</span></div><h1>Action Now</h1><div class="desc">Ein Undercover-Ermittler flieht vor seiner Vergangenheit – und gerät mitten in einen internationalen Schlagabtausch.</div><div class="btns"><button class="btn btn-main" data-act="toast" data-t="Live-Übertragung startet">Jetzt ansehen</button><button class="btn btn-ghost" data-act="toast" data-t="Weitere Infos">Details</button></div></div></div>';
function favRow(chs,withTitle){return (withTitle?'<div class="sec"><h2>Deine Favoriten</h2><span class="all">Zum Live-TV</span></div>':'')+'<div class="cards">'+chs.map((j)=>'<div class="lcard" data-act="play" data-i="'+j+'">'+logoHtml(LIVE[j])+'<div><h4>'+esc(LIVE[j][0])+'</h4><div class="sub">'+esc(nowProg(j)?nowProg(j).t:'–')+'</div></div><span class="hd">HD</span></div>').join('')+'</div>';}
function postRow(offs){return '<div class="cards">'+POSTS.slice(offs,offs+5).map((p,j)=>'<div class="post" style="background:'+postArt(offs+j)+'" data-act="toast" data-t="'+esc(p[0])+' startet"><span class="tg">'+p[1]+'</span><p>'+esc(p[0])+'</p></div>').join('')+'</div>';}
function liveRow(chs){return '<div class="cards">'+chs.map((j,l)=>'<div class="tile" data-act="play" data-i="'+j+'"><span class="tnum">'+LIVE[j][5]+'</span><span class="tdot"></span><h4>'+esc(LIVE[j][0])+'</h4><small>'+esc(nowProg(j)?nowProg(j).t:'–')+'</small></div>').join('')+'</div>';}
function musicRow(){const icons=['♪','◉','☊','♬','♫','☻','✦','♩'];
return '<div class="cards">'+MIX.map((m,j)=>'<div class="lcard" data-act="toast" data-t="Stream: '+esc(m[0])+'"><div class="icon" style="font-size:22px">'+icons[j%icons.length]+'</div><div><h4>'+esc(m[0])+'</h4><div class="sub">'+m[1]+'</div></div>'+(j<2?'<span class="hd">LIVE</span>':'')+'</div>').join('')+'</div>';}
function buildHome(){
const pad=$('#homePad');let h='';
if(S.homet==='hub'){
h=heroHtml+
hSection('Weiter schauen','Zum Live-TV',favRow(favList()))+
hSection('Jetzt LIVE','Sender',liveRow([16,17,18,19]))+
hSection('Empfohlen für dich','Mehr ansehen',postRow(0))+
hSection('Musik & Radio','Alle Sender',musicRow());
}else{
h='<div class="fbanner"><div class="tag">ZENPLAYER</div><h1>Alles live im Blick</h1><p>26 Sender, EPG-Programm und deine Lieblingssendungen – an einem Ort.</p></div>'+
hSection('Jetzt LIVE','Sender',liveRow([16,17,18,19]))+
hSection('Weiter schauen','Zum Live-TV',favRow(favList()))+
hSection('Empfohlen für dich','Mehr ansehen',postRow(0));
}
pad.innerHTML=h;
}
function buildBrowse(n){
const pad=$('#browsePad');
if(n==='music'){pad.innerHTML='<div class="head"><h1>Musik & Radio</h1></div>'+musicRow();return;}
if(n==='vod'){pad.innerHTML='<div class="fbanner"><div class="tag">NUVIO</div><h1>Videos auf Abruf</h1><p>Nuvio-Integration folgt – bis dahin siehst du hier eine Auswahl an Sendungen.</p></div><div class="sec"><h2>Empfohlen</h2></div>'+postRow(0)+postRow(5);return;}
const rows=LIVE.map((c,i)=>{const p=nowProg(i),st=hh(p?p.s:0),et=hh(p?p.e:0),pc=clamp((nowMin()-(p?p.s:0))/((p?p.e:0)-(p?p.s:0)||1),0,1)*100;return '<div class="clrow'+(i===S.play.ch?' live':'')+'" data-act="play" data-i="'+i+'">'+logoHtml(c)+'<div class="cn"><b>'+esc(c[0])+'</b><small>'+c[5]+' · HD</small></div><div class="prog"><small>'+st+' – '+et+'</small><b>'+esc(p?p.t:'–')+'</b><div class="bar"><i style="width:'+pc+'%"></i></div></div><div class="time">'+st+'</div></div>';}).join('');
pad.innerHTML='<div class="head"><h1>Live-TV</h1></div>'+rows;
}
const TABS=[['darstellung','Erscheinungsbild'],['wiedergabe','Wiedergabe'],['profil','Profil']];
let setTab='darstellung';
function buildSettings(){
const pad=$('#setPad');
pad.innerHTML='<div class="head"><h1>Einstellungen</h1></div><div class="subbar">'+TABS.map(t=>'<div class="subp'+(t[0]===setTab?' on':'')+'" data-set="'+t[0]+'">'+t[1]+'</div>').join('')+'</div><div id="setBody"></div>';
renderSetTab();
}
function setSetTab(t){setTab=t;const sb=$('#setBody');if(sb){$$('[data-set]').forEach(e=>e.classList.toggle('on',e.dataset.set===t));renderSetTab();}}
function pvMini(){const t=themeById(S.theme);const f=S.fx;
return '<div class="pvbox"><div class="pvb pv1"></div><div class="pvb pv2"></div><div class="pvb pv3"></div><div class="pv-hero"><h3>Dein Fernsehen</h3><p>'+t.n+' · Fokus „'+({ring:'Ring',glow:'Glühen',bar:'Balken',zoom:'Aufblähen',corners:'Ecken',halo:'Halo',sweep:'Schimmer'})[f]+'“ · Glas '+S.glass+'%</p><span class="pvplay">► Ansehen</span><div class="pv-chips">'+[4,9,17].map(i=>'<div class="pv-chip">'+logoHtml(LIVE[i])+'<span>'+LIVE[i][0]+'</span></div>').join('')+'</div></div></div>';}
function swatchHtml(t){const on=S.theme===t.id;return '<div class="thwrap'+(on?' on':'')+'" data-th="'+t.id+'" title="'+t.n+'"><span class="thg" style="background:linear-gradient(135deg,'+t.a+', '+t.b+')"></span><b>'+t.n+'</b></div>';}
function scRow(k,l){return '<div class="prow"><label>'+l+'</label><button class="zbtn" data-scr="'+k+'" data-d="-">−</button><b class="v">'+Math.round(S.scale[k]*100)+'%</b><button class="zbtn" data-scr="'+k+'" data-d="+">+</button></div>';}
function renderSetTab(){
const body=$('#setBody');if(!body)return;let h='';
if(setTab==='darstellung'){
h='<div class="panel"><label style="font-size:12px;font-weight:700">Live-Vorschau</label>'+pvMini()+'</div>'+
'<div class="panel"><label style="font-size:12px;font-weight:700;margin-bottom:2px">Farbthema</label><div class="prow">'+THEMES.map(swatchHtml).join('')+'</div></div>'+
'<div class="panel"><div class="prow"><label>Fokus-Markierung</label>'+[['ring','Ring'],['glow','Glühen'],['bar','Balken'],['zoom','Aufblähen'],['corners','Ecken'],['halo','Halo'],['sweep','Schimmer']].map(f=>'<button class="zbtn'+(S.fx===f[0]?' on':'')+'" data-app="fx" data-v="'+f[0]+'">'+f[1]+'</button>').join('')+'</div><div class="prow"><label>Karten-Stil</label>'+[['grad','Verlauf'],['flat','Flach'],['glass','Glas']].map(u=>'<button class="zbtn'+(S.ui===u[0]?' on':'')+'" data-app="ui" data-v="'+u[0]+'">'+u[1]+'</button>').join('')+'</div></div>'+
'<div class="panel"><div class="prow"><label>Navigation</label>'+[['rail','Seitenleiste'],['top','Obere Leiste'],['tiles','Kacheln']].map(n=>'<button class="zbtn'+(S.nav===n[0]?' on':'')+'" data-app="nav" data-v="'+n[0]+'">'+n[1]+'</button>').join('')+'</div><div class="prow"><label>Home-Stil</label>'+[['hub','Hub'],['kanal','Kanal']].map(x=>'<button class="zbtn'+(S.homet===x[0]?' on':'')+'" data-app="homet" data-v="'+x[0]+'">'+x[1]+'</button>').join('')+'</div></div>'+
'<div class="panel"><label style="font-size:12px;font-weight:700">Glas &amp; Hintergrund</label>'+
'<div class="prow"><label>Glasstärke</label><button class="zbtn" data-app="glass" data-d="-">−</button><b class="v">'+S.glass+'%</b><button class="zbtn" data-app="glass" data-d="+">+</button></div>'+
'<div class="prow"><label>Wallpaper-Unschärfe</label><button class="zbtn" data-app="wblur" data-d="-">−</button><b class="v">'+S.wblur+'</b><button class="zbtn" data-app="wblur" data-d="+">+</button></div>'+
'<div class="prow"><label>Hintergrund</label>'+[['wave','Welle'],['aurora','Aurora'],['grad','Farblauf'],['holo','Raster'],['plain','Dezent']].map(b=>'<button class="zbtn'+(S.bg===b[0]?' on':'')+'" data-app="bg" data-v="'+b[0]+'">'+b[1]+'</button>').join('')+'</div>'+
'<div class="prow"><label>Animierte Hintergründe</label><button class="zbtn'+(S.animbg?' on':'')+'" data-app="animbg">'+(S.animbg?'AN':'AUS')+'</button></div>'+
'<div class="prow"><label>Schriftgröße</label>'+[['0','Klein'],['1','Normal'],['2','Groß']].map(f2=>'<button class="zbtn'+(String(S.font)===f2[0]?' on':'')+'" data-app="font" data-v="'+f2[0]+'">'+f2[1]+'</button>').join('')+'</div></div>'+
'<div class="panel"><label style="font-size:12px;font-weight:700">Größen &amp; Programm</label>'+['home','browse','epg','player','settings'].map(k=>scRow(k,({home:'Start',browse:'Videos &amp; Live',epg:'TV-Programm',player:'Player',settings:'Einstellungen'})[k])).join('')+
'<div class="prow"><label>Programm-Größe</label><button class="zbtn" data-app="esize" data-d="-">−</button><b class="v">'+Math.round(S.epg.size*100)+'%</b><button class="zbtn" data-app="esize" data-d="+">+</button></div>'+
'<div class="prow"><label>Senderleiste</label><button class="zbtn" data-app="colw" data-d="-">−</button><b class="v">'+Math.round(S.epg.colw*100)+'%</b><button class="zbtn" data-app="colw" data-d="+">+</button></div>'+
'<div class="prow"><label>Dichte</label>'+[['0','Kompakt'],['1','Standard'],['2','Groß']].map(d=>'<button class="zbtn'+(S.density===+d[0]?' on':'')+'" data-app="density" data-v="'+d[0]+'">'+d[1]+'</button>').join('')+'</div>'+
[['logos','Senderlogos anzeigen'],['pipp','Vorschau nach OK'],['autonow','Auf „Jetzt“ springen'],['favsonly','Nur Favoriten']].map(k=>'<div class="prow"><label>'+k[1]+'</label><button class="zbtn'+(S.epg[k[0]]?' on':'')+'" data-tog="'+k[0]+'">'+(S.epg[k[0]]?'AN':'AUS')+'</button></div>').join('')+
'<p style="font-size:10.5px;color:var(--dim);margin-top:10px">◄ ► oder Bild-auf/ab wechselt im Programm den Tag (bis 6 Tage voraus).</p></div>'+
'<div class="panel"><label style="font-size:12px;font-weight:700">Einrichtung teilen</label><div class="prow"><button class="zbtn" data-app="gencode">Code erzeugen</button><button class="zbtn" data-app="copycode">Kopieren</button></div><div class="codebox" id="shareOut">Code noch nicht erzeugt</div><div class="prow"><div class="shareIn"><input id="shareIn" placeholder="Code hier einfügen" maxlength="600"></div><button class="zbtn" data-app="applycode">Übernehmen</button></div></div>';
}
else if(setTab==='wiedergabe'){h='<div class="panel"><div class="prow"><label>Player-Engine</label><button class="zbtn'+(S.play.eng==='exo'?' on':'')+'" data-eng="exo">ExoPlayer</button><button class="zbtn'+(S.play.eng==='vlc'?' on':'')+'" data-eng="vlc">VLC</button></div><div class="prow"><label>Qualität</label>'+['720p','1080p','4K'].map(q=>'<button class="zbtn'+(S.play.q===q?' on':'')+'" data-q="'+q+'">'+q+'</button>').join('')+'</div><div class="prow"><label>Sender-Info beim Zappen</label><button class="zbtn'+(S.play.zinfo?' on':'')+'" data-tog="zinfo">'+(S.play.zinfo?'AN':'AUS')+'</button></div><div class="prow"><label>Overlay ausblenden nach</label><button class="zbtn" data-scr="autohide" data-d="-">−</button><b class="v">'+S.play.autohide.toFixed(1)+' s</b><button class="zbtn" data-scr="autohide" data-d="+">+</button></div></div>';}
else if(setTab==='profil'){h='<div class="panel"><div class="prow"><div class="avatarTest" id="av">'+esc(S.prof.nick.slice(0,1).toUpperCase()||'Z')+'</div><div class="nickbox"><input id="nickIn" value="'+esc(S.prof.nick)+'" maxlength="16" placeholder="Dein Nickname"></div></div></div><div class="panel"><div class="prow"><label>Deinen Zustand zurücksetzen</label><button class="zbtn danger" data-cmd="reset">Reset</button></div></div>';}
body.innerHTML=h;
if(setTab==='profil'){const inp=$('#nickIn');if(inp){inp.addEventListener('input',()=>{S.prof.nick=inp.value||'Gast';const av=$('#av');if(av)av.textContent=S.prof.nick.slice(0,1).toUpperCase();ST.save();});}}
applyScale();
refocus();
}
function codeObj(){return {v:2,theme:S.theme,fx:S.fx,glass:S.glass,bg:S.bg,animbg:S.animbg,font:S.font,density:S.density,nav:S.nav,homet:S.homet,ui:S.ui,wblur:S.wblur,epg:{size:S.epg.size,colw:S.epg.colw,favsonly:S.epg.favsonly}};}
function toCode(o){const s=JSON.stringify(o);let b=btoa(unescape(encodeURIComponent(s)));b=b.replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');return b;}
function fromCode(c){let b=String(c).replace(/-/g,'+').replace(/_/g,'/');while(b.length%4)b+='=';const s=decodeURIComponent(escape(atob(b)));return JSON.parse(s);}
function getShareCode(){return toCode(codeObj());}
function applyShareCode(c){
try{const o=fromCode(c);if(!o||(o.v!==1&&o.v!==2))return false;
['theme','fx','glass','bg','animbg','font','density','nav','homet','ui','wblur'].forEach(k=>{if(o[k]!==undefined)S[k]=o[k];});
if(o.epg){['size','colw','favsonly'].forEach(k=>{if(o.epg[k]!==undefined)S.epg[k]=o.epg[k];});}
applyAll();buildHome();if(S.screen==='settings')renderSetTab();if(S.screen==='epg')buildEpg();ST.save();return true;
}catch(e){return false;}}
const EPG_W=24;
const DENS=[{eph:64,ech:148,etw:108,eps:0.95},{eph:80,ech:178,etw:130,eps:1},{eph:98,ech:214,etw:156,eps:1.12}];
const den=()=>{const b=DENS[S.density]||DENS[1],z=S.epg.size||1,cw=S.epg.colw||1;
return {eph:Math.round(b.eph*z),ech:Math.round(b.ech*cw),etw:Math.round(b.etw*z),eps:b.eps};};
function isFav(vi){return (S.favs||[]).indexOf(vi)>=0;}
function toggleFav(vi){let f=S.favs&&S.favs.length?[...S.favs]:[0,2,4,6];const l=f.indexOf(vi);if(l>=0)f.splice(l,1);else f.push(vi);S.favs=f;ST.save();}
function favList(){return (S.favs&&S.favs.length?S.favs:[0,2,4,6]).filter(j=>LIVE[j]).slice(0,8);}
function currentVis(){const all=LIVE.map((c,i)=>i);if(!S.epg.favsonly||!S.favs||!S.favs.length)return all;return all.filter(i=>isFav(i));}
function buildEpg(){
const el=$('#epgPad');const now=nowMin();const e=den();
const vis=currentVis();
const nw=Math.max(0,Math.round((now-300)/60*e.etw));
el.innerHTML='<div class="epg-wrap"><div class="epg-top"><div class="et-t">TV-PROGRAMM<small>'+dLabel()+'</small><span class="et-clk" id="etClock"><svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><circle cx="12" cy="12" r="9"></circle><path d="M12 7v5l3 2"></path></svg><b>'+pad(now/60|0)+':'+pad(now%60)+'</b></span></div><div class="et-dnav"><button class="zbtn" data-app="prevday" title="Vortag">◀</button><b class="et-daylbl">'+dLabel()+'</b><button class="zbtn" data-app="nextday" title="Nächster Tag">▶</button></div><div class="et-hint">Pause = jetzt · ◄ ► = Tag</div></div>'+
'<div class="epg-body"><div class="chcol"><div class="chhead">SENDER<span>'+vis.length+'</span></div><div class="chscrol" id="chScrol">'+
vis.map((ci)=>'<div class="chrow'+(S.play.ch===ci?' cur':'')+'" data-i="'+ci+'">'+logoHtml(LIVE[ci],'sm')+'<div class="nm"><b>'+esc(LIVE[ci][0])+'</b><small>'+esc(nowProg(ci)?nowProg(ci).t:'–')+'</small></div><span class="no">'+LIVE[ci][5]+'</span>'+(isFav(ci)?'<span class="favdot">♥</span>':'')+'</div>').join('')+
'</div></div><div class="gwrap"><div class="gscrol" id="gScrol"><div class="grid">'+
'<div class="ghead">'+Array.from({length:EPG_W},(_,h2)=>'<div class="gc'+((h2+5)%24===Math.floor(now/60)?' cur':'')+'">'+('0'+((h2+5)%24)).slice(-2)+':00</div>').join('')+'</div>'+
vis.map((ci)=>'<div class="erow" data-i="'+ci+'">'+progsFor(ci,S.epg.day).map((p,j)=>{const l=(p.s-300)/60*e.etw,w=(p.e-p.s)/60*e.etw-4,isNow=p.s<=now&&p.e>now;
const wi=Math.round(w),gn=({news:'Info',doku:'Doku',serie:'Serie',show:'Show',sport:'Sport',film:'Film',kids:'Kinder'})[CHCATS[ci]]||'';
return '<div class="pg'+(isNow?' now':'')+'" data-i="'+ci+'" data-p="'+j+'" title="'+esc(p.t)+'" style="left:'+Math.round(l)+'px;width:'+wi+'px"><small>'+hh(p.s)+(wi>124?' – '+hh(p.e):'')+'</small><b>'+esc(p.t)+'</b>'+(wi>170?'<em>'+gn+'</em>':'')+(isNow?'<span class="pbar"><i></i></span>':'')+'</div>';}).join('')+'</div>').join('')+
'<div class="nowline" style="left:'+nw+'px"></div>'+
'</div></div></div></div></div>';
attachEpgScroll();
if(!vis.length){S.row=0;S.ecol=0;return;}
if(S.row===undefined||S.row>=vis.length)S.row=0;
S.ecol=progAt(vis[S.row],nowMin());
markEpgFocus(F.layer==='grid'?'grid':'chs');
updatePip();
}
function attachEpgScroll(){
const cs=$('#chScrol'),gs=$('#gScrol');if(!cs||!gs)return;if(cs.dataset.synced)return;cs.dataset.synced='1';
const sy1=()=>{if(gs.scrollTop!==cs.scrollTop)gs.scrollTop=cs.scrollTop;};
const sy2=()=>{if(cs.scrollTop!==gs.scrollTop)cs.scrollTop=gs.scrollTop;};
cs.addEventListener('scroll',sy1);gs.addEventListener('scroll',sy2);
cs.addEventListener('wheel',e=>{e.preventDefault();e.stopPropagation();sy1();},{passive:false});
gs.addEventListener('wheel',e=>{e.preventDefault();e.stopPropagation();sy2();},{passive:false});
const pad=$('#epgPad');pad.removeEventListener('wheel',wheelSync);pad.addEventListener('wheel',wheelSync,{passive:false});
}
function wheelSync(e){
const gs=$('#gScrol'),cs=$('#chScrol');if(!gs||!cs)return;
e.preventDefault();
if(e.shiftKey||e.ctrlKey||e.altKey){gs.scrollLeft+=e.deltaY;return;}
gs.scrollTop+=e.deltaY;cs.scrollTop=gs.scrollTop;
}
function markEpgFocus(mode){
const vis=currentVis();if(!vis.length)return;
S.row=clamp(S.row,0,vis.length-1);
$$('.chrow').forEach((e2,i)=>e2.classList.toggle('act',i===S.row));
$$('.erow').forEach((e2,i)=>e2.classList.toggle('act',i===S.row));
const vi=vis[S.row];
if(mode!=='chs')$$('.pg').forEach(p=>p.classList.toggle('foc',+p.dataset.i===vi&&+p.dataset.p===S.ecol));
setCh(vi);
const gs=$('#gScrol'),cs=$('#chScrol');if(!gs)return;
const e=den();
const target=Math.max(0,S.row*(e.eph+5)-gs.clientHeight/2+e.eph/2);
gs.scrollTop=clamp(target,0,Math.max(0,gs.scrollHeight-gs.clientHeight));
cs.scrollTop=gs.scrollTop;
if(mode!=='chs'){const pg=$('.pg.foc');if(pg){const bp=gs.getBoundingClientRect(),pr=pg.getBoundingClientRect();gs.scrollLeft+=pr.left-bp.left-gs.clientWidth*0.5;}}
}
function enterEpgGrid(vi){
const vis=currentVis();const r=vis.indexOf(vi);if(r<0)return;
S.row=r;S.ecol=progAt(vis[S.row],nowMin());S.anchorT=nowMin();
F.layer='grid';setF(null);markEpgFocus('grid');
}
function setDay(d){
S.epg.day=clamp(S.epg.day+d,0,6);ST.save();buildEpg();
toast(S.epg.day===0?'Heute':S.epg.day===1?'Morgen':'In '+S.epg.day+' Tagen');
const vis=currentVis();if(!vis.length)return;
if(F.layer==='chs'){const ch=$('.chrow.act');setF(ch||null);}
}
function epgMove(k){
const vis=currentVis();if(!vis.length)return;
if(k==='ArrowDown')S.row=clamp(S.row+1,0,vis.length-1);
else if(k==='ArrowUp')S.row=clamp(S.row-1,0,vis.length-1);
else if(k==='ArrowRight')S.ecol=clamp((S.ecol||0)+1,0,progsFor(vis[S.row],S.epg.day).length-1);
else if(k==='ArrowLeft')S.ecol=clamp((S.ecol||1)-1,0,progsFor(vis[S.row],S.epg.day).length-1);
if(k==='ArrowUp'||k==='ArrowDown'){if(S.epg.autonow)S.ecol=progAt(vis[S.row],nowMin());else S.ecol=clamp(S.ecol||0,0,progsFor(vis[S.row],S.epg.day).length-1);}
const pr=progsFor(vis[S.row],S.epg.day)[S.ecol];S.anchorT=pr?pr.s:nowMin();
markEpgFocus('grid');
}
function chsMove(k){
const vis=currentVis();if(!vis.length)return;
if(k==='ArrowDown')S.row=clamp(S.row+1,0,vis.length-1);
else if(k==='ArrowUp')S.row=clamp(S.row-1,0,vis.length-1);
else return;
const pr=progsFor(vis[S.row],S.epg.day)[S.ecol];S.anchorT=pr?pr.s:nowMin();
markEpgFocus('chs');
const ch=$('.chrow.act');if(ch)setF(ch);
}
function snapNow(){
const vis=currentVis();if(!vis.length)return;
const r=vis.indexOf(S.play.ch);S.row=r<0?0:r;
S.ecol=progAt(vis[S.row],nowMin());
S.anchorT=nowMin();
if(F.layer==='grid'){markEpgFocus('grid');}
else{markEpgFocus('chs');const ch=$('.chrow.act');if(ch)setF(ch);}
toast('Springe zu jetzt');
}
let entT=null,entFired=false,lastEnt=0;
function epgEnterDown(e){
if(e&&e.repeat)return;
const now=Date.now();
if(now-lastEnt<480){lastEnt=0;if(entT){clearTimeout(entT);entT=null;}doEpkEnter();return;}
lastEnt=now;entFired=false;
entT=setTimeout(()=>{entFired=true;entT=null;openCtx();},520);
}
function epgEnterUp(){if(entT){clearTimeout(entT);entT=null;if(!entFired)doEpkEnter();}}
function doEpkEnter(){
const vi=currentVis()[S.row];if(vi===undefined)return;
if(previewOn){previewOn=false;updatePip();openPlayer();return;}
if(!S.epg.pipp){openPlayer();return;}
previewOn=true;setCh(vi);updatePip();
}
const F={layer:'nav',item:null,lastC:null,lastCKey:'',_ctxBack:null};
function keyOf(el){
const d=el.dataset;if(!d)return '';
if(d.app)return 'app:'+d.app+(d.v?':'+d.v:'')+(d.d?':'+d.d:'');
if(d.tog)return 'tog:'+d.tog;
if(d.scr)return 'scr:'+d.scr+':'+(d.d||'');
if(d.th)return 'th:'+d.th;
if(d.eng)return 'eng:'+d.eng;
if(d.q)return 'q:'+d.q;
if(d.cmd)return 'cmd:'+d.cmd;
if(d.set)return 'set:'+d.set;
if(d.cat)return 'cat:'+d.cat;
if(d.nav)return 'nav:'+d.nav;
if(el.id)return 'id:'+el.id;
return el.className||'';
}
function fItems(){
if(F.layer==='nav')return $$('.nitem[data-nav],.titem[data-nav],.ktile[data-nav]').filter(el=>el.getClientRects().length>0);
if(S.screen==='epg'&&F.layer==='chs')return $$('.chrow').filter(el=>el.getClientRects().length>0);
if(S.screen==='epg')return [];
const sel='.lcard,.post,.sq,.tile,.wc,.cr,.btn,.fbanner,.clrow,.catp,.thwrap,.zbtn,.subp,.pchip,.shareIn input';
return $$(sel).filter(el=>el.getClientRects().length>0);
}
function setF(el){if(F.item&&F.item!==el&&F.item.classList)F.item.classList.remove('kf');if(el){el.classList.add('kf');F.item=el;}else F.item=null;F.lastC=F.item;F.lastCKey=F.item?keyOf(F.item):'';}
function enterContent(){
setF(null);
if(S.screen==='epg'){const vis=currentVis();if(!vis.length)return;F.layer='chs';markEpgFocus('chs');const ch=$('.chrow.act');setF(ch||$('.chrow')||null);return;}
const items=fItems();let next=null;
if(F.lastCKey)next=items.find(el=>keyOf(el)===F.lastCKey)||null;
if(!next&&F.lastC&&F.lastC.isConnected)next=F.lastC;
setF(next||items[0]);F.lastC=F.item;F.lastCKey=F.item?keyOf(F.item):'';
}
function refocus(){if(S.screen!=='settings'||F.layer!=='content'||F.layer==='context')return;const items=fItems();let next=F.lastCKey?items.find(el=>keyOf(el)===F.lastCKey):null;setF(next||items[0]||null);}
function leaveToNav(){
setF(null);F.layer='nav';
const items=fItems();const i2=items.findIndex(el=>el.dataset.nav===S.navSel);
setF(items[Math.max(0,i2)]);
}
function mv2d(k,items,cur){
if(!cur||!cur.isConnected)return items[0];
const cr=cur.getBoundingClientRect();let best=null,bs=1e9;
items.forEach(el=>{if(el===cur)return;const r=el.getBoundingClientRect();
const dx=r.left-cr.left,dy=r.top-cr.top;let ok=false;
if(k==='ArrowRight')ok=dy>-r.height*.6&&dy<r.height*.6&&dx>0;
else if(k==='ArrowLeft')ok=dy>-r.height*.6&&dy<r.height*.6&&dx<0;
else if(k==='ArrowDown')ok=dx>-r.width*.6&&dx<r.width*.6&&dy>0;
else if(k==='ArrowUp')ok=dx>-r.width*.6&&dx<r.width*.6&&dy<0;
if(ok){const d=Math.abs(dx)+Math.abs(dy);if(d<bs){bs=d;best=el;}}});return best||cur;
}
let inCtx=false;
function ctxFocusKeys(k){
const its=$$('#ctxBox .ctxit');if(!its.length)return;
let i=its.indexOf(F.item);if(i<0)i=0;
if(k==='ArrowDown'||k==='ArrowRight')i=(i+1)%its.length;
else if(k==='ArrowUp'||k==='ArrowLeft')i=(i-1+its.length)%its.length;
else if(k==='Enter'||k===' '){its[i].click();return;}
setF(its[i]);
}
function openCtx(){
const vis=currentVis();const vi=S.screen==='epg'?(vis[S.row]!==undefined?vis[S.row]:vis[0]):S.play.ch;
const ch=LIVE[vi];const p=S.screen==='epg'?progsFor(vi,S.epg.day)[S.ecol]:nowProg(vi);
const replay=p&&p.s<=nowMin()&&p.e>nowMin();
const items=[
{a:'play',i:'M18 6l-8 4 8 4V6z',t:'Wiedergabe',s:'OK'},
{a:'fav',i:'M12 21s-7-4.6-9.6-8.7C1 9.6 2.6 6 6.2 6c1.9 0 3.5 1.1 4.6 2.8.1.2.3.2.4 0C12.3 7.1 13.9 6 15.8 6c3.6 0 5.2 3.6 3.8 6.3C17 14.4 12 21 12 21z',t:isFav(vi)?'Aus Favoriten entfernen':'Zu Favoriten hinzufügen',s:'♥',fav:1}
];
if(replay)items.push({a:'replay',i:'M6 4v18M6 4a1 1 0 011-1h9l4 4v13a1 1 0 01-1 1H7a1 1 0 01-1-1z',t:'Von Anfang an (Replay)',s:'ENTF'});
items.push({a:'remind',i:'M12 3l2.4 5.4 5.9.5-4.5 3.9 1.3 5.8L12 16l-5.1 2.6 1.3-5.8L3.7 8.9l5.9-.5z',t:'Merken',s:'M'});
items.push({a:'info',i:'M12 8v5M12 16.5V16',t:'Senderinfo',s:'I'});
items.push({a:'rec',i:'M12 2a10 10 0 100 20 10 10 0 000-20zM12 6v6l4 2',t:'Aufnahme planen',s:'R'});
if(vi>0)items.push({a:'foco',i:'M19 12H5M12 19l-7-7 7-7',t:'Nur dieser Sender',s:'ESC'});
const box=$('#ctxBox');
box.innerHTML='<div class="ctxTitle"><h3>'+esc(ch[0])+'</h3><small>'+(p?hh(p.s)+' – '+hh(p.e)+' · '+esc(p.t):'')+'</small></div>'+items.map(it=>'<div class="ctxit" data-a="'+it.a+'"><span class="ico"><svg viewBox="0 0 24 24"><path d="'+it.i+'"/></svg></span>'+esc(it.t)+'<small>'+it.s+'</small></div>').join('');
$('#ctxWrap').classList.add('show');inCtx=true;F._ctxBack={layer:F.layer,el:F.item,row:S.row,ecol:S.ecol};F.layer='context';const its=$$('#ctxBox .ctxit');if(its.length)setF(its[0]);
}
function hideCtx(){
$('#ctxWrap').classList.remove('show');inCtx=false;const b=F._ctxBack||{};setF(null);
F.layer=b.layer||'content';
if(b.el&&b.el.isConnected&&F.layer!=='grid'){setF(b.el);}
else if(S.screen==='epg'){const vis=currentVis();if(b.row!==undefined&&b.row<vis.length&&vis.length){S.row=b.row;markEpgFocus(F.layer==='chs'?'chs':'grid');if(F.layer==='chs'){const ch=$('.chrow.act');if(ch)setF(ch);}}}
F._ctxBack=null;
}
function navKey(k){
const items=fItems();if(!items.length)return;
let i=items.findIndex(el=>el.dataset.nav===S.navSel);if(i<0)i=0;
if(k==='ArrowDown')i=(i+1)%items.length;
else if(k==='ArrowUp')i=(i-1+items.length)%items.length;
else if(k==='ArrowRight'||k==='Enter'||k===' '){S.navSel=items[i].dataset.nav;setF(null);openNav(S.navSel);enterContent();return;}
else return;
S.navSel=items[i].dataset.nav;setF(items[i]);
}
function contentKey(k){
const items=fItems();if(!items.length)return;
if(['ArrowRight','ArrowLeft','ArrowUp','ArrowDown'].includes(k))setF(mv2d(k,items,F.item&&F.item.isConnected?F.item:null));
else if(k==='Enter'||k===' '){if(F.item&&F.item.click)F.item.click();}
else if(k==='c'||k==='C')openCtx();
}
function epgKeys(k){
if(F.layer==='nav'){navKey(k);return;}
if(['PageDown',']','MediaFastForward','MediaTrackNext'].includes(k)){setDay(1);return;}
if(['PageUp','[','MediaRewind','MediaTrackPrevious'].includes(k)){setDay(-1);return;}
if(F.layer==='chs'){
if(k==='ArrowRight'||k==='Enter'||k===' '){const vis=currentVis();if(!vis.length)return;enterEpgGrid(vis[S.row]);return;}
if(k==='c'||k==='C'){openCtx();return;}
chsMove(k);return;}
if(F.layer==='grid'){
if(['ArrowRight','ArrowLeft','ArrowUp','ArrowDown'].includes(k))epgMove(k);
else if(k==='Enter'||k===' '){epgEnterDown(null);}
else if(k==='c'||k==='C')openCtx();
}
}
function kRoute(ev){
const k=ev.key;
if(k==='MediaPlayPause'||k==='Pause'){if(S.screen==='player'){zapPlay();return;}if(S.screen==='epg'&&F.layer!=='nav'){snapNow();return;}return;}
if(S.screen==='player'){playerKey(ev);return;}
if(inCtx){if(k==='Escape'||k==='Backspace')hideCtx();else ctxFocusKeys(k);return;}
if(k==='Backspace'){ev.preventDefault();keyBack();return;}
if(['ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(k))ev.preventDefault();
if(S.screen==='epg'){epgKeys(k);return;}
if(F.layer==='nav'){navKey(k);return;}
contentKey(k);
}
function keyBack(){
if(S.screen==='player'){$('#zInfo').classList.remove('show');if(document.body.classList.contains('pmin')){closePlayer();return;}if($('#pOverlay')&&!document.body.classList.contains('pmin')){document.body.classList.add('pmin');return;}closePlayer();return;}
if(inCtx){hideCtx();return;}
if(S.screen==='epg'){
if(previewOn&&F.layer==='grid'){previewOn=false;updatePip();return;}
if(F.layer==='grid'){F.layer='chs';markEpgFocus('chs');const ch=$('.chrow.act');setF(ch||null);return;}
}
if(F.layer==='nav'){enterContent();return;}
leaveToNav();
}
let pminT=null;
function openPlayer(){S._prev=S.screen==='player'?S._prev:S.screen;S.screen='player';previewOn=false;$$('.screen').forEach(e=>e.classList.toggle('on',e.id==='s-player'));document.body.classList.remove('pmin');setCh(S.play.ch);setF(null);F.layer='content';pokePmin();startClock();}
function closePlayer(){S.screen=S._prev||'epg';stopClock();openNav(S._prev||'epg');enterContent();}
function pokePmin(){if(pminT)clearTimeout(pminT);document.body.classList.remove('pmin');pminT=setTimeout(()=>document.body.classList.add('pmin'),S.play.autohide*1000);}
function togglePmin(){document.body.classList.toggle('pmin');}
function startClock(){if(clockT)clearInterval(clockT);tickClock();clockT=setInterval(tickClock,1000);}
function stopClock(){if(clockT)clearInterval(clockT);clockT=null;}
function tickClock(){const hms=pad(nowMin()/60|0)+':'+pad(nowMin()%60);const pc=$('#pClock');if(pc)pc.textContent=hms;const ec=$('#etClock');if(ec)ec.querySelector('b').textContent=hms;}
function zapPlay(){const zp=$('#zPlay');const on=zp.getAttribute('data-paused')==='1';zp.setAttribute('data-paused',on?'0':'1');zp.textContent=on?'▶':'❚❚';toast(on?'Pause aufgehoben':'Pause');}
let zT=null;
function zinfo(k){if(!S.play.zinfo)return;const d=$('#zInfo');d.classList.remove('show');void d.offsetWidth;d.classList.add('show');if(zT)clearTimeout(zT);zT=setTimeout(()=>d.classList.remove('show'),2200);}
function playerKey(ev){
const k=ev.key;
if(k==='ArrowUp'){setCh(S.play.ch-1);zinfo(1);return;}
if(k==='ArrowDown'){setCh(S.play.ch+1);zinfo(1);return;}
if(k==='Enter'||k===' '){togglePmin();pokePmin();return;}
if(k==='Escape'){if($('#zInfo').classList.contains('show')){$('#zInfo').classList.remove('show');return;}if(document.body.classList.contains('pmin')){document.body.classList.remove('pmin');return;}closePlayer();return;}
if(k==='MediaPlayPause'||k==='Pause'){zapPlay();pokePmin();return;}
}
document.addEventListener('keydown',e=>{kRoute(e);});
document.addEventListener('keyup',e=>{if((e.key==='Enter'||e.key===' ')){if(entT){clearTimeout(entT);entT=null;if(!entFired)doEpkEnter();}}});
function focusCell(ci,jv){const vis=currentVis();const r=vis.indexOf(ci);if(r<0)return;S.row=r;S.ecol=clamp(jv,0,progsFor(ci,S.epg.day).length-1);S.anchorT=progsFor(ci,S.epg.day)[jv]?progsFor(ci,S.epg.day)[jv].s:nowMin();F.layer='grid';setF(null);markEpgFocus('grid');}
function applyApp(f,v){
if(f==='fx'){S.fx=v;applyFx();renderSetTab();}
else if(f==='nav'){S.nav=v;applyNav();renderSetTab();}
else if(f==='homet'){S.homet=v;renderSetTab();if(S.screen==='home')buildHome();}
else if(f==='glass'){S.glass=clamp(S.glass+((v==='+'?1:-1)*10),0,100);applyGlass();if(S.screen==='settings')renderSetTab();}
else if(f==='wblur'){S.wblur=clamp(S.wblur+((v==='+'?1:-1)*10),0,90);applyGlass();if(S.screen==='settings')renderSetTab();}
else if(f==='esize'){S.epg.size=clamp(Math.round((S.epg.size+(v==='+'?0.1:-0.1))*10)/10,0.6,1.5);applyDensity();if(S.screen==='epg')buildEpg();if(S.screen==='settings')renderSetTab();}
else if(f==='colw'){S.epg.colw=clamp(Math.round((S.epg.colw+(v==='+'?0.1:-0.1))*10)/10,0.7,1.4);applyDensity();if(S.screen==='epg')buildEpg();if(S.screen==='settings')renderSetTab();}
else if(f==='ui'){S.ui=v;applyUi();renderSetTab();if(S.screen==='epg')buildEpg();}
else if(f==='bg'){S.bg=v;applyBg();if(S.screen==='settings')renderSetTab();}
else if(f==='animbg'){S.animbg=S.animbg?0:1;applyBg();if(S.screen==='settings')renderSetTab();}
else if(f==='font'){S.font=+v;applyFont();if(S.screen==='settings')renderSetTab();}
else if(f==='density'){S.density=+v;applyDensity();if(S.screen==='epg')buildEpg();if(S.screen==='settings')renderSetTab();}
ST.save();
}
document.addEventListener('click',e=>{
let t=e.target;
if(t.closest('#ctxWrap')){const it=t.closest('.ctxit');
if(it){const a=it.dataset.a;const vis=currentVis();const cvi=S.screen==='epg'?(vis[S.row]!==undefined?vis[S.row]:vis[0]):S.play.ch;const pp=S.screen==='epg'?progsFor(cvi,S.epg.day)[S.ecol]:nowProg(cvi);hideCtx();
if(a==='play'){setCh(cvi);openPlayer();}
else if(a==='fav'){toggleFav(cvi);toast(isFav(cvi)?'Zu Favoriten hinzugefügt':'Aus Favoriten entfernt');if(S.screen==='epg')buildEpg();else if(S.screen==='home')buildHome();}
else if(a==='replay'){toast('Replay ab Sendungsbeginn gestartet');}
else if(a==='remind'){toast('Zur Merkliste hinzugefügt');}
else if(a==='info'){toast(LIVE[cvi][0]+' · Kanal '+LIVE[cvi][5]+' · '+(pp?pp.t:'–'));}
else if(a==='rec'){toast('Aufnahme um '+(pp?hh(pp.s):'–')+' Uhr geplant');}
else if(a==='foco'){toast('Fokus auf '+LIVE[cvi][0]);}}
else{hideCtx();}
return;}
const cr=t.closest('.chrow');if(cr){const vi=+cr.dataset.i;
focusCell(vi,S.epg.autonow&&vi===currentVis()[S.row]?S.ecol:progAt(vi,nowMin()));
if(e.detail>=2){openPlayer();}
else if(previewOn){previewOn=false;openPlayer();}
else{previewOn=true;setCh(vi);updatePip();}
return;}
const pg=t.closest('.pg');if(pg){const vi=+pg.dataset.i,jv=+pg.dataset.p;
const wasFoc=vi===currentVis()[S.row]&&jv===S.ecol;
focusCell(vi,jv);
if(e.detail>=2){openPlayer();}
else if(wasFoc){doEpkEnter();}
else{if(!previewOn)previewOn=true;setCh(vi);updatePip();}
return;}
const nav=t.closest('[data-nav]');if(nav){S.navSel=nav.dataset.nav;setF(null);openNav(nav.dataset.nav);enterContent();return;}
const pin=t.closest('[data-act]');if(pin){const act=pin.dataset.act;
if(act==='play'){const vi=+pin.dataset.i;if(pin.classList.contains('clrow')){$$('.clrow').forEach(c=>c.classList.toggle('live',c===pin));}setCh(vi);if(e.detail>=2){openPlayer();}else if(previewOn){previewOn=false;openPlayer();}return;}
if(act==='toast'){toast(pin.dataset.t||'Aktion');return;}}
const th=t.closest('[data-th]');if(th){S.theme=th.dataset.th;applyTheme();$$('.thwrap').forEach(w=>w.classList.toggle('on',w===th));renderSetTab();ST.save();return;}
const ap=t.closest('[data-app]');if(ap){const f=ap.dataset.app;
if(f==='prevday'){setDay(-1);}
else if(f==='nextday'){setDay(1);}
else if(f==='gencode'){const ob=$('#shareOut');if(ob)ob.textContent=getShareCode();toast('Code erzeugt');}
else if(f==='copycode'){const ob=$('#shareOut');if(ob){const tx=ob.textContent;if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(tx).then(()=>toast('Code kopiert'),()=>toast('Code kopiert'));}else toast('Code kopiert');}}
else if(f==='applycode'){const inp=$('#shareIn');const ok=applyShareCode(inp?inp.value:'');toast(ok?'Einstellungen übernommen':'Ungültiger Code');renderSetTab();}
else applyApp(f,ap.dataset.v);
return;}
const scr=t.closest('[data-scr]');if(scr){const k=scr.dataset.scr,d=scr.dataset.d;
if(k==='autohide'){S.play.autohide=clamp(Math.round((S.play.autohide+(d==='+'?0.5:-0.5))*10)/10,2,10);renderSetTab();}
else{S.scale[k]=clamp(Math.round((S.scale[k]+(d==='+'?0.1:-0.1))*10)/10,0.7,1.6);applyScale();renderSetTab();}
ST.save();return;}
const tog=t.closest('[data-tog]');if(tog){const k=tog.dataset.tog;
if(k==='zinfo')S.play.zinfo=S.play.zinfo?0:1;
else if(S.epg[k]!==undefined)S.epg[k]=S.epg[k]?0:1;
renderSetTab();if(S.screen==='epg')buildEpg();ST.save();return;}
const eng=t.closest('[data-eng]');if(eng){S.play.eng=eng.dataset.eng;renderSetTab();ST.save();return;}
const q=t.closest('[data-q]');if(q){S.play.q=q.dataset.q;renderSetTab();ST.save();return;}
const cat=t.closest('[data-cat]');if(cat){$$('.catp').forEach(x=>x.classList.toggle('on',x===cat));return;}
const dset=t.closest('[data-set]');if(dset){setSetTab(dset.dataset.set);enterContent();return;}
const btn=t.closest('.pback');if(btn){keyBack();return;}
const zz=t.closest('#zPrev');if(zz){setCh(S.play.ch-1);zinfo(1);pokePmin();return;}
const zn=t.closest('#zNext');if(zn){setCh(S.play.ch+1);zinfo(1);pokePmin();return;}
const zp=t.closest('#zPlay');if(zp){zapPlay();pokePmin();return;}
const pp=t.closest('.cbtn');if(pp){pokePmin();return;}
});
document.addEventListener('pointerdown',e=>{const pg=e.target.closest('.pg, .chrow');if(!pg)return;let moved=false;
const fi=setTimeout(()=>{if(moved)return;openCtx();},520);
const up=()=>{clearTimeout(fi);document.removeEventListener('pointerup',up);document.removeEventListener('pointermove',mv);};
const mv=ev=>{if(Math.abs(ev.clientX-e.clientX)>5||Math.abs(ev.clientY-e.clientY)>5)moved=true;};
document.addEventListener('pointerup',up);document.addEventListener('pointermove',mv);
});
document.addEventListener('contextmenu',e=>{e.preventDefault();if(e.target.closest('.pg')||e.target.closest('.chrow')||e.target.closest('.clrow')||e.target.closest('.nitem')||e.target.closest('.lcard')||e.target.closest('.tile'))openCtx();});
function init(){ST.load();applyAll();S.screen='home';S.nav='home';buildHome();setCh(S.play.ch);F.layer='nav';S.navSel='home';const n=fItems();setF(n[0]||null);}
init();
window.__z={openNav:openNav,openPlayer:openPlayer,closePlayer:closePlayer,setCh:setCh,curCh:curCh,epgMove:epgMove,chsMove:chsMove,enterEpgGrid:enterEpgGrid,setDay:setDay,snapNow:snapNow,openCtx:openCtx,hideCtx:hideCtx,setSetTab:setSetTab,buildEpg:buildEpg,buildHome:buildHome,applyAll:applyAll,getShareCode:getShareCode,applyShareCode:applyShareCode,toast:toast,zinfo:zinfo,zapPlay:zapPlay,isFav:isFav,toggleFav:toggleFav,favList:favList,currentVis:currentVis,getS:()=>S,
layer:()=>F.layer,setLayer:function(l){F.layer=l;if(l==='nav')leaveToNav();else enterContent();},
navSel:()=>S.navSel,preview:()=>previewOn,
tlk:function(k){kRoute({key:k,preventDefault(){}});},
setApp:applyApp,
setTheme:function(id){S.theme=id;applyTheme();ST.save();},
};
window.addEventListener('load',()=>{startClock();});
})();