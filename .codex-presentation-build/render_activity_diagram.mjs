import fs from 'node:fs/promises';
import sharp from 'sharp';

const out = '/Users/meetbhoi/Downloads/V1/placement-management-system/presentations/CampusHire_Activity_Diagram.png';
const W = 2800, H = 1600;
const colors = {
  bg: '#F4F7FB', lane: '#FFFFFF', navy: '#0B1F3A', ink: '#182B49', slate: '#5B6B7F', line: '#B9C9DB',
  blue: '#1F5FAF', cyan: '#4CA7D8', pale: '#EAF2FB', green: '#1F8A70', greenPale: '#E8F6F0', amber: '#D7942B', amberPale: '#FFF2DF', red: '#B64949', redPale: '#FCECEC'
};
const esc = (value) => String(value).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const lines = (value) => String(value).split('\n');
function text(x, y, value, size=22, fill=colors.ink, weight=400, anchor='middle') {
  const rows = lines(value); const offset = (rows.length - 1) * size * 0.55;
  return rows.map((row, i) => `<text x="${x}" y="${y + i*size - offset}" text-anchor="${anchor}" font-family="Arial, sans-serif" font-size="${size}" font-weight="${weight}" fill="${fill}">${esc(row)}</text>`).join('');
}
function rect(x,y,w,h,fill=colors.pale,stroke=colors.cyan,rx=16) { return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" stroke="${stroke}" stroke-width="3"/>`; }
function node(x,y,w,h,label,kind='action') { const palette = kind==='decision' ? [colors.amberPale,colors.amber] : kind==='error' ? [colors.redPale,colors.red] : kind==='store' ? [colors.greenPale,colors.green] : kind==='start' || kind==='end' ? [kind==='start'?colors.navy:colors.green,kind==='start'?colors.navy:colors.green] : [colors.pale,colors.cyan]; const r = kind==='decision' ? `<polygon points="${x+w/2},${y} ${x+w},${y+h/2} ${x+w/2},${y+h} ${x},${y+h/2}" fill="${palette[0]}" stroke="${palette[1]}" stroke-width="3"/>` : rect(x,y,w,h,palette[0],palette[1],kind==='start'||kind==='end'?h/2:16); return r + text(x+w/2,y+h/2+8,label,kind==='decision'?20:20,kind==='start'||kind==='end'?colors.bg:colors.ink,700); }
function arrow(x1,y1,x2,y2,label='') { const mx=(x1+x2)/2; const d=`M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`; return `<path d="${d}" fill="none" stroke="${colors.blue}" stroke-width="4" marker-end="url(#arrow)"/>` + (label ? text(mx, Math.min(y1,y2)-12,label,17,colors.slate,700) : ''); }
function straight(x1,y1,x2,y2,label='') { return `<path d="M ${x1} ${y1} L ${x2} ${y2}" fill="none" stroke="${colors.blue}" stroke-width="4" marker-end="url(#arrow)"/>` + (label ? text((x1+x2)/2,(y1+y2)/2-12,label,17,colors.slate,700) : ''); }
let svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}"><defs><marker id="arrow" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto"><path d="M0,0 L12,6 L0,12 z" fill="${colors.blue}"/></marker></defs><rect width="${W}" height="${H}" fill="${colors.bg}"/>`;
svg += text(60,58,'CampusHire UML Activity Diagram',60,colors.navy,700,'start');
svg += text(60,94,'Student application and staff recruitment workflow',26,colors.slate,400,'start');
const lanes = [
  ['STUDENT',100,230,colors.green], ['ANGULAR STAFF DASHBOARD / DTPO',330,310,colors.blue], ['UTPO',640,160,colors.amber], ['SPRING BOOT BACKEND',800,430,colors.cyan], ['POSTGRESQL',1230,300,colors.green]
];
for (const [label,y,h,accent] of lanes) { svg += `<rect x="30" y="${y}" width="2740" height="${h}" rx="18" fill="${colors.lane}" stroke="${colors.line}" stroke-width="2"/>`; svg += `<rect x="30" y="${y}" width="280" height="${h}" rx="18" fill="${accent}"/>`; svg += text(170,y+45,label,22,colors.bg,700); }

// Student lane
svg += node(350,170,170,68,'Start','start');
svg += node(590,160,250,88,'Login with\nenrollment number','action');
svg += node(920,160,260,88,'Browse published\ndrives','action');
svg += node(1260,160,230,88,'Choose drive','action');
svg += node(1580,160,270,88,'View application\nand round progress','action');
svg += node(1950,170,170,68,'End','end');
svg += straight(520,204,590,204); svg += straight(840,204,920,204); svg += straight(1180,204,1260,204); svg += straight(1490,204,1580,204); svg += straight(1850,204,1950,204);

// Staff lane
svg += node(360,405,245,76,'Create company /\ndraft drive','action');
svg += node(700,405,230,76,'Publish draft\ndrive','action');
svg += node(1025,405,275,76,'Configure ordered\nrounds','action');
svg += node(1395,405,250,76,'Load current\ncandidates','action');
svg += node(1740,405,245,76,'Record round\nresult','action');
svg += node(2080,405,245,76,'Publish round\nresults','action');
svg += node(2420,405,250,76,'Record final\noutcome','action');
svg += straight(605,443,700,443); svg += straight(930,443,1025,443); svg += straight(1300,443,1395,443); svg += straight(1645,443,1740,443); svg += straight(1985,443,2080,443); svg += straight(2325,443,2420,443);

// UTPO lane
svg += node(390,685,300,70,'University-wide staff scope','action');
svg += node(850,685,350,70,'Create / list DTPO coordinators','action');
svg += node(1380,685,360,70,'Manage university-wide drives','action');
svg += straight(690,720,850,720); svg += straight(1200,720,1380,720);

// Backend lane
svg += node(360,865,275,76,'Authenticate and\nreload account','action');
svg += node(735,865,190,76,'Valid?','decision');
svg += node(1040,865,220,76,'Reject login','error');
svg += node(1390,865,210,76,'Student?','decision');
svg += node(1750,850,320,106,'Filter by published status,\ndepartment and eligibility','action');
svg += node(2180,865,220,76,'Eligible?','decision');
svg += node(2510,850,220,106,'Check unique\napplication','action');
svg += straight(635,903,735,903); svg += straight(925,903,1040,903,'No'); svg += straight(925,903,1390,903,'Yes'); svg += straight(1600,903,1750,903,'Student'); svg += straight(2070,903,2180,903); svg += straight(2400,903,2510,903,'Yes');
svg += node(1750,1000,320,70,'Reject visibility / application','error');
svg += node(2180,1000,220,70,'Duplicate?','decision');
svg += node(2510,1000,220,70,'Persist APPLIED','store');
svg += straight(2180,941,1910,1000,'No'); svg += straight(2290,941,2290,1000); svg += straight(2400,1035,2510,1035,'No');
svg += node(1040,1000,220,70,'Staff scope','action');
svg += node(1390,1000,300,70,'DTPO department or UTPO','action');
svg += node(1750,1000,260,70,'Authorized?','decision');
svg += node(2070,1000,220,70,'Forbidden','error');
svg += straight(1500,941,1150,1000,'Staff'); svg += straight(1260,1035,1390,1035); svg += straight(1690,1035,1750,1035); svg += straight(2010,1035,2070,1035,'No');
svg += node(2300,1120,360,70,'Validate draft, active year,\ncompany and deadline','action');
svg += node(1800,1120,250,70,'Publishable?','decision');
svg += node(1500,1120,220,70,'Reject publish','error');
svg += node(920,1120,320,70,'Round candidate rule:\nfirst round or passed previous','action');
svg += node(510,1120,250,70,'Final passed?','decision');
svg += node(120,1120,300,70,'Await result / keep status','action');
svg += node(120,1330,300,70,'Persist SELECTED or REJECTED','store');
svg += straight(1750,1035,1800,1120); svg += straight(1800,1155,1500,1155,'No'); svg += straight(2050,1155,2300,1155,'Yes');
svg += straight(2300,1190,1080,1190); svg += straight(1080,1190,1045,1120); svg += straight(920,1155,760,1155); svg += straight(510,1155,420,1155,'No'); svg += straight(510,1190,270,1330,'Yes');

// Database lane
svg += node(360,1300,340,90,'users / roles / students','store');
svg += node(850,1300,390,90,'companies / academic_years / drives','store');
svg += node(1400,1300,360,90,'placement_applications','store');
svg += node(1930,1300,470,90,'placement_drive_rounds / round_results','store');
svg += straight(495,941,530,1300); svg += straight(480,443,950,1300); svg += straight(2660,1035,1580,1300); svg += straight(2080,443,2165,1300);
svg += text(60,1550,'Backend authorization and eligibility remain the source of truth. Arrows marked No terminate or reject the current action; later-round candidates require a passed previous round.',26,colors.slate,400,'start');
svg += '</svg>';
await sharp(Buffer.from(svg)).png({ compressionLevel: 9 }).toFile(out);
console.log(JSON.stringify({out,width:W,height:H}));
