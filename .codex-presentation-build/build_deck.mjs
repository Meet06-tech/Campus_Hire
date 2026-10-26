import fs from 'node:fs/promises';
import path from 'node:path';
import { Presentation, PresentationFile } from '@oai/artifact-tool';

const workspaceDir = '/Users/meetbhoi/Downloads/V1/placement-management-system';
const skillDir = '/Users/meetbhoi/.codex/plugins/cache/openai-primary-runtime/presentations/26.1022.11436/skills/presentations';
const buildDir = path.join(workspaceDir, '.codex-presentation-build');
const outputDir = path.join(workspaceDir, '.codex-presentation-output');
const candidatePath = path.join(buildDir, 'CampusHire_Project_Presentation_With_Diagrams_candidate.pptx');
const finalPath = path.join(workspaceDir, 'presentations', 'CampusHire_Project_Presentation_With_Diagrams.pptx');

const W = 1280, H = 720;
const C = {
  navy: '#0B1F3A', blue: '#1F5FAF', cyan: '#4CA7D8', ink: '#182B49', slate: '#5B6B7F',
  light: '#F4F7FB', pale: '#EAF2FB', white: '#FFFFFF', line: '#D6E1EE', green: '#1F8A70',
  amber: '#D7942B', red: '#B64949', muted: '#8A98AA'
};
const FONT = 'Arial';

async function imgBytes(file) {
  const b = await fs.readFile(file);
  return b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength);
}

function shape(slide, geometry, left, top, width, height, fill='none', lineFill='none', lineWidth=0, extra={}) {
  return slide.shapes.add({ geometry, position: { left, top, width, height }, fill, line: { style: 'solid', fill: lineFill, width: lineWidth }, ...extra });
}
function text(slide, value, left, top, width, height, style={}) {
  const s = shape(slide, 'textbox', left, top, width, height, 'none', 'none', 0);
  s.text = value;
  s.text.style = { typeface: FONT, fontSize: 18, color: C.ink, ...style };
  s.text.insets = 0;
  return s;
}
function line(slide, x1, y1, x2, y2, color=C.line, width=2) {
  return shape(slide, 'line', x1, y1, Math.max(1, x2-x1), Math.max(1, y2-y1), 'none', color, width);
}
function box(slide, title, body, x, y, w, h, accent=C.blue, opts={}) {
  shape(slide, 'roundRect', x, y, w, h, opts.fill || C.white, opts.line || C.line, 1, { borderRadius: 'rounded-xl', shadow: opts.shadow ? 'shadow-sm' : undefined });
  shape(slide, 'rect', x, y, 6, h, accent, 'none', 0);
  text(slide, title, x+22, y+18, w-38, 28, { fontSize: 20, bold: true, color: C.navy });
  if (body) text(slide, body, x+22, y+54, w-38, h-64, { fontSize: opts.bodySize || 16, color: C.slate, breakLine: true });
}
function bulletText(items) { return items.map(x => `• ${x}`).join('\n'); }
function slideBase(presentation, title, eyebrow, number, dark=false) {
  const slide = presentation.slides.add();
  slide.background.fill = dark ? C.navy : C.light;
  if (!dark) {
    shape(slide, 'rect', 0, 0, W, 10, C.blue, 'none', 0);
    text(slide, eyebrow.toUpperCase(), 64, 34, 420, 22, { fontSize: 12, bold: true, color: C.blue, letterSpacing: 1.5 });
    text(slide, title, 64, 63, 980, 55, { fontSize: 32, bold: true, color: C.navy });
    line(slide, 64, 130, 1216, 130, C.line, 1);
  } else {
    text(slide, eyebrow.toUpperCase(), 64, 38, 520, 22, { fontSize: 12, bold: true, color: '#A9D7F5', letterSpacing: 1.5 });
    text(slide, title, 64, 70, 1040, 64, { fontSize: 38, bold: true, color: C.white });
  }
  text(slide, String(number).padStart(2, '0'), 1168, 665, 48, 20, { fontSize: 11, bold: true, color: dark ? '#A9D7F5' : C.muted, align: 'right' });
  text(slide, 'CampusHire · Placement Management System', 64, 665, 500, 20, { fontSize: 11, color: dark ? '#A9D7F5' : C.muted });
  return slide;
}
function notes(slide, value) { slide.speakerNotes.text = value; slide.speakerNotes.setVisible(true); }

const p = Presentation.create({ slideSize: { width: W, height: H } });
const charusat = await imgBytes(path.join(workspaceDir, 'presentations/assets/charusat_logo.jpeg'));
const cmpica = await imgBytes(path.join(workspaceDir, 'presentations/assets/cmpica_logo.jpeg'));

// 1 Title
{
  const s = p.slides.add(); s.background.fill = C.navy;
  shape(s, 'rect', 0, 0, 14, H, C.cyan, 'none', 0);
  shape(s, 'rect', 760, 0, 520, H, '#123C68', 'none', 0);
  s.images.add({ blob: charusat, contentType: 'image/jpeg', alt: 'CHARUSAT logo', fit: 'contain', position: { left: 82, top: 52, width: 110, height: 110 } });
  s.images.add({ blob: cmpica, contentType: 'image/jpeg', alt: 'CMPICA logo', fit: 'contain', position: { left: 210, top: 52, width: 110, height: 110 } });
  text(s, 'CAMPUSHIRE', 82, 205, 600, 38, { fontSize: 16, bold: true, color: '#A9D7F5', letterSpacing: 3 });
  text(s, 'Placement\nManagement System', 82, 254, 620, 145, { fontSize: 48, bold: true, color: C.white });
  text(s, 'A unified platform for student placement discovery, coordination, applications and recruitment progress.', 84, 430, 560, 70, { fontSize: 21, color: '#D7E8F7' });
  text(s, 'CHARUSAT University · CMPICA', 84, 584, 480, 26, { fontSize: 16, bold: true, color: C.white });
  text(s, 'Project review presentation · 2026–27', 84, 617, 480, 22, { fontSize: 14, color: '#A9D7F5' });
  text(s, '01', 1168, 665, 48, 20, { fontSize: 11, bold: true, color: '#A9D7F5', align: 'right' });
  notes(s, 'Opening: CampusHire centralizes the placement journey for students and the university placement office. Source: repository README, backend controllers and current frontend structures.');
}

// 2 Introduction
{
  const s = slideBase(p, 'Placement coordination in one workspace', 'Introduction', 2);
  text(s, 'CampusHire connects the people, records and decisions involved in campus recruitment.', 64, 166, 710, 56, { fontSize: 25, bold: true, color: C.navy });
  box(s, 'For students', 'Discover eligible published drives\nApply once per drive\nTrack applications and round progress', 64, 270, 330, 220, C.cyan);
  box(s, 'For department TPOs', 'Create and publish department drives\nManage candidates and recruitment rounds\nKeep scope inside the assigned department', 475, 270, 330, 220, C.blue);
  box(s, 'For the university TPO', 'Coordinate university-wide activity\nManage department coordinators\nSee cross-department placement data', 886, 270, 330, 220, C.green);
  notes(s, 'Frame the system as a shared source of truth. Student and staff experiences use separate clients but the same backend contracts.');
}

// 3 Problem
{
  const s = slideBase(p, 'The placement problem', 'System analysis', 3);
  const left = 'Manual coordination spreads information across messages, spreadsheets and separate records.';
  text(s, left, 64, 172, 500, 72, { fontSize: 26, bold: true, color: C.navy });
  const issues = [
    ['Fragmented records', 'Company, drive and student information can drift apart.'],
    ['Unclear eligibility', 'Students need a reliable answer about which drives they can see.'],
    ['Department boundaries', 'DTPO work must remain limited to the assigned department.'],
    ['Slow status updates', 'Students need visibility into applications and round outcomes.']
  ];
  issues.forEach((it, i) => { const y=170+i*105; shape(s,'ellipse',650,y,54,54,C.pale,'none',0); text(s,String(i+1),650,y+13,54,28,{fontSize:21,bold:true,color:C.blue,align:'center'}); text(s,it[0],730,y+2,380,26,{fontSize:20,bold:true,color:C.navy}); text(s,it[1],730,y+32,430,44,{fontSize:16,color:C.slate}); });
  notes(s, 'Explain the project motivation without claiming measured performance improvements. These are workflow problems the system is designed to address.');
}

// 4 Objectives
{
  const s = slideBase(p, 'Project objectives', 'Objectives', 4);
  const objs = [
    ['01', 'Secure identity', 'Support students, DTPOs and UTPOs with JWT authentication and role-aware access.'],
    ['02', 'Structured placement data', 'Keep companies, drives, applications and outcomes connected in PostgreSQL.'],
    ['03', 'Department-aware access', 'Enforce DTPO scope in backend services, not only in the UI.'],
    ['04', 'Transparent progress', 'Expose published recruitment results to the student who owns the application.']
  ];
  objs.forEach((o,i)=>{ const x=64+(i%2)*585, y=170+Math.floor(i/2)*190; box(s,o[0]+' · '+o[1],o[2],x,y,540,145,[C.cyan,C.blue,C.green,C.amber][i]); });
  notes(s, 'These objectives summarize the implemented direction and the current recruitment-round milestone.');
}

// 5 Existing vs proposed
{
  const s = slideBase(p, 'Existing workflow and proposed platform', 'System analysis', 5);
  const table=s.tables.add({rows:6,columns:3,left:64,top:170,width:1152,height:390,values:[['Area','Fragmented workflow','CampusHire approach'],['Student discovery','Messages and scattered notices','Published drives filtered by eligibility'],['Applications','Separate forms or records','One application per student and drive'],['Staff scope','Manual department filtering','Backend authorization for DTPO and UTPO'],['Recruitment progress','Status updates are difficult to trace','Ordered rounds with publishable results'],['Reporting','Repeated manual compilation','Live staff lists and dashboard aggregates']]});
  table.styleOptions={headerRow:true,bandedRows:true}; table.borders.assign({style:'solid',fill:C.line,width:1});
  for(let c=0;c<3;c++){table.getCell(0,c).fill=C.navy;table.getCell(0,c).text.style={typeface:FONT,fontSize:16,bold:true,color:C.white};}
  for(let r=1;r<6;r++)for(let c=0;c<3;c++)table.getCell(r,c).text.style={typeface:FONT,fontSize:15,color:c===0?C.navy:C.slate,bold:c===0};
  notes(s,'Comparison is qualitative and based on the project’s stated workflow goals. It does not claim measured improvements.');
}

// 6 Roles
{
  const s = slideBase(p, 'Roles and access boundaries', 'Scope and roles', 6);
  const table=s.tables.add({rows:4,columns:4,left:64,top:176,width:1152,height:300,values:[['Role','Primary view','Management scope','Student-facing effect'],['STUDENT','Flutter app','Own profile and applications','Sees eligible published drives'],['DTPO','Angular dashboard','Assigned department','Creates department drives and manages rounds'],['UTPO','Angular dashboard','University-wide','Manages coordinators and university-wide activity']]});
  table.styleOptions={headerRow:true,bandedRows:true}; table.borders.assign({style:'solid',fill:C.line,width:1});
  for(let c=0;c<4;c++){table.getCell(0,c).fill=C.blue;table.getCell(0,c).text.style={typeface:FONT,fontSize:15,bold:true,color:C.white};}
  for(let r=1;r<4;r++)for(let c=0;c<4;c++)table.getCell(r,c).text.style={typeface:FONT,fontSize:14,color:c===0?C.navy:C.slate,bold:c===0};
  text(s,'The server derives scope from the authenticated account and its department assignment.',64,522,900,38,{fontSize:20,bold:true,color:C.navy});
  notes(s,'Role names are persisted as UTPO, DTPO and STUDENT. UTPO has no department assignment; DTPO requires one.');
}

// 7 Stack
{
  const s = slideBase(p, 'Technology stack', 'Implementation', 7);
  const stack=[['Flutter','Student mobile experience','Student drives, applications and profile'],['Angular','Staff workspace','UTPO and DTPO dashboards'],['Spring Boot','REST API and business rules','Authentication, placement and recruitment services'],['PostgreSQL','Persistent data store','Users, departments, drives, applications and results'],['JWT + BCrypt','Security foundation','Signed sessions and password hashing']];
  stack.forEach((x,i)=>{ const y=160+i*88; shape(s,'roundRect',64,y,236,62,C.pale,'none',0,{borderRadius:'rounded-lg'}); text(s,x[0],84,y+13,195,25,{fontSize:20,bold:true,color:C.blue}); text(s,x[1],340,y+8,290,24,{fontSize:18,bold:true,color:C.navy}); text(s,x[2],660,y+8,500,40,{fontSize:16,color:C.slate}); line(s,304,y+31,327,y+31,C.cyan,2); });
  notes(s,'Versions verified in the repository include Java 21 and Spring Boot 3.5.6. PostgreSQL is configured through environment variables.');
}

// 8 Architecture
{
  const s = slideBase(p, 'System architecture', 'Architecture', 8);
  const nodes=[['Student app','Flutter'],['Staff workspace','Angular'],['REST API','Spring Boot'],['Database','PostgreSQL']];
  const xs=[80,80,470,870], ys=[190,430,300,300];
  nodes.forEach((n,i)=>{shape(s,'roundRect',xs[i],ys[i],270,92,i<2?C.pale:C.white,C.line,1,{borderRadius:'rounded-xl',shadow:'shadow-sm'}); text(s,n[0],xs[i]+20,ys[i]+18,230,28,{fontSize:21,bold:true,color:C.navy,align:'center'}); text(s,n[1],xs[i]+20,ys[i]+52,230,22,{fontSize:15,color:C.blue,align:'center'});});
  line(s,350,236,470,343,C.cyan,3); line(s,350,476,470,343,C.cyan,3); line(s,740,346,870,346,C.cyan,3);
  text(s,'JWT-secured HTTP requests',382,232,230,22,{fontSize:14,color:C.slate,align:'center'}); text(s,'JPA repositories and migrations',742,312,220,22,{fontSize:14,color:C.slate,align:'center'});
  text(s,'The clients stay thin: visibility, eligibility and authorization are enforced by the backend.',300,585,680,36,{fontSize:20,bold:true,color:C.navy,align:'center'});
  notes(s,'Architecture reflects the actual repository structure: Flutter and Angular clients call the Spring Boot API, which persists to PostgreSQL.');
}

// 9 Use case diagram
{
  const s = slideBase(p, 'Use case diagram', 'Requirements and actors', 9);
  shape(s, 'roundRect', 300, 160, 720, 430, C.white, C.line, 1, { borderRadius: 'rounded-xl' });
  text(s, 'CampusHire Placement Management System', 330, 178, 660, 30, { fontSize: 22, bold: true, color: C.navy, align: 'center' });
  const cases = [
    ['Authenticate / view /api/me', 365, 230, 250, 46],
    ['Manage profile and photo', 365, 300, 250, 46],
    ['Browse eligible published drives', 365, 370, 250, 46],
    ['Apply and track progress', 365, 440, 250, 46],
    ['Manage companies and drives', 705, 230, 250, 46],
    ['Publish drives', 705, 300, 250, 46],
    ['Manage rounds and results', 705, 370, 250, 46],
    ['Create / list DTPO coordinators', 705, 440, 250, 46]
  ];
  cases.forEach(([label,x,y,w,h]) => { shape(s, 'ellipse', x, y, w, h, C.pale, C.cyan, 1); text(s, label, x+10, y+12, w-20, h-20, { fontSize: 14, color: C.navy, align: 'center' }); });
  const actors = [['STUDENT', 72, 285, C.green], ['DTPO', 1060, 300, C.blue], ['UTPO', 1060, 470, C.amber]];
  actors.forEach(([label,x,y,accent]) => { shape(s, 'roundRect', x, y, 150, 56, accent, 'none', 0, { borderRadius: 'rounded-lg' }); text(s, label, x+10, y+16, 130, 25, { fontSize: 17, bold: true, color: C.white, align: 'center' }); });
  [[222,313,365,253],[222,313,365,323],[222,313,365,393],[222,313,365,463],[955,253,1060,328],[955,323,1060,328],[955,393,1060,328],[955,463,1060,498]].forEach(a=>line(s,...a,C.cyan,2));
  text(s, 'Server-side role and department scope applies to every protected use case.', 310, 612, 660, 28, { fontSize: 17, color: C.slate, align: 'center' });
  notes(s, 'This use case view reflects the implemented actors and endpoints. Students own profile and application actions. DTPOs operate inside their assigned department. UTPOs have university-wide scope and coordinator management.');
}

// 10 Activity diagram
{
  const s = slideBase(p, 'Activity diagram: application and recruitment flow', 'Business workflow', 10);
  const flow = [
    ['Start', 80, 235, 125, 48, C.navy],
    ['Authenticate', 245, 235, 150, 48, C.blue],
    ['Load published\neligible drives', 440, 210, 175, 72, C.cyan],
    ['Choose drive', 660, 235, 145, 48, C.blue],
    ['Eligibility +\nduplicate check', 850, 210, 180, 72, C.amber],
    ['Persist APPLIED', 1065, 235, 150, 48, C.green]
  ];
  flow.forEach(([label,x,y,w,h,accent],i) => { shape(s, i===0?'ellipse':'roundRect', x, y, w, h, accent, 'none', 0, i===0 ? {} : { borderRadius: 'rounded-lg' }); text(s,label,x+10,y+12,w-20,h-20,{fontSize:15,bold:true,color:C.white,align:'center'}); if(i<flow.length-1) line(s,x+w,y+h/2,flow[i+1][1],flow[i+1][2]+flow[i+1][4]/2,C.cyan,3); });
  shape(s, 'diamond', 515, 395, 150, 88, C.white, C.amber, 2); text(s, 'Eligible?', 540, 426, 100, 24, { fontSize: 17, bold: true, color: C.navy, align: 'center' });
  shape(s, 'roundRect', 300, 405, 150, 60, '#FFF2DF', C.amber, 1, { borderRadius: 'rounded-lg' }); text(s, 'Reject request', 315, 425, 120, 22, { fontSize: 15, bold: true, color: C.red, align: 'center' });
  line(s, 527, 282, 575, 395, C.cyan, 2); text(s, 'no', 468, 347, 34, 18, { fontSize: 13, color: C.red, align: 'center' }); line(s, 665, 439, 790, 439, C.cyan, 2); text(s, 'yes', 693, 412, 40, 18, { fontSize: 13, color: C.green, align: 'center' });
  shape(s, 'roundRect', 80, 555, 250, 62, C.white, C.line, 1, { borderRadius: 'rounded-lg' }); text(s, 'Student views application\nand published progress', 95, 570, 220, 36, { fontSize: 15, color: C.navy, align: 'center' });
  shape(s, 'roundRect', 420, 555, 250, 62, C.white, C.line, 1, { borderRadius: 'rounded-lg' }); text(s, 'Staff configures rounds\nand selects candidates', 435, 570, 220, 36, { fontSize: 15, color: C.navy, align: 'center' });
  shape(s, 'roundRect', 760, 555, 250, 62, C.white, C.line, 1, { borderRadius: 'rounded-lg' }); text(s, 'Record results → publish\nround → final outcome', 775, 570, 220, 36, { fontSize: 15, color: C.navy, align: 'center' });
  text(s, 'The backend remains the source of truth for eligibility, uniqueness and authorization.', 280, 642, 720, 22, { fontSize: 16, color: C.slate, align: 'center' });
  notes(s, 'The activity flow shows two connected journeys. Student application is gated by published status, eligibility and the unique student-drive constraint. After applications exist, staff manage ordered rounds and publish outcomes for students to see.');
}

// 11 Class diagram
{
  const s = slideBase(p, 'Class diagram: core placement model', 'Domain model', 11);
  const classes = [
    ['UserAccount', 'userId\nrole\ndepartment'],
    ['Department', 'departmentId\nname / code'],
    ['Student', 'studentId\nenrollmentNumber\nacademicProgram'],
    ['Company', 'companyId\ncompanyName\ncontacts'],
    ['PlacementDrive', 'driveId\nstatus\neligibility\nacademicYear'],
    ['PlacementProgram', 'programId\nprogramCode'],
    ['PlacementApplication', 'applicationId\nappliedAt\nstatus'],
    ['RecruitmentRound', 'roundId\nroundOrder\nresultsPublished'],
    ['RoundResult', 'roundResultId\nresult\nrecordedAt']
  ];
  const pos = [[60,180],[60,390],[285,390],[285,180],[520,180],[520,390],[785,180],[785,390],[1030,390]];
  classes.forEach((v,i)=>{const [x,y]=pos[i]; shape(s,'roundRect',x,y,190,108,C.white,C.line,1,{borderRadius:'rounded-lg',shadow:'shadow-sm'}); shape(s,'rect',x,y,190,8,[C.blue,C.green,C.cyan,C.amber,C.blue,C.cyan,C.green,C.amber,C.red][i],'none',0); text(s,v[0],x+10,y+18,170,24,{fontSize:16,bold:true,color:C.navy,align:'center'}); text(s,v[1],x+16,y+50,158,46,{fontSize:12,color:C.slate,align:'center'});});
  [[155,288,155,390],[250,250,285,420],[250,444,285,444],[475,234,520,234],[710,234,785,234],[615,288,615,390],[710,270,785,444],[975,444,1030,444]].forEach(a=>line(s,...a,C.cyan,2));
  text(s,'optional profile',163,326,105,18,{fontSize:12,color:C.slate,align:'center'}); text(s,'assigned dept.',245,372,100,18,{fontSize:12,color:C.slate,align:'center'}); text(s,'many drives',390,205,90,18,{fontSize:12,color:C.slate,align:'center'}); text(s,'Company / academic year / department → drive',510,145,560,22,{fontSize:16,bold:true,color:C.blue,align:'center'}); text(s,'Drive ↔ program uses placement_drive_programs; applications and round results remain linked to the drive.',250,612,780,28,{fontSize:16,color:C.slate,align:'center'});
  notes(s, 'This class diagram uses the actual domain entities and relationships. UserAccount optionally has a Student profile; a PlacementDrive belongs to a company and academic year, may be department-scoped, and is connected to applications, programs and recruitment rounds.');
}

// 12 Modules
{
  const s = slideBase(p, 'Functional modules', 'System modules', 12);
  const mods=[['Identity','Registration, login, JWT, /api/me'],['Student profile','Profile details and photo storage'],['Companies','Staff-managed company records'],['Placement drives','Draft, publish, eligibility and visibility'],['Applications','Apply once and view status'],['Recruitment rounds','Ordered rounds, results and progression'],['Dashboards','UTPO/DTPO operations and aggregates']];
  mods.forEach((m,i)=>{const x=64+(i%3)*390,y=170+Math.floor(i/3)*140; box(s,m[0],m[1],x,y,350,108,[C.cyan,C.blue,C.green,C.amber,C.red,C.cyan,C.blue][i],{bodySize:15});});
  notes(s,'Modules are derived from controllers, services, entity mappings and the two frontend clients.');
}

// 10 DB design
{
  const s = slideBase(p, 'Database design', 'Data model', 13);
  const e=[['users','user_id\nrole\ndepartment_id'],['departments','department_id\nname / code'],['students','student_id\nenrollment / program'],['companies','company_id\ncompany_name'],['placement_drives','drive_id\nstatus / eligibility'],['placement_applications','application_id\nstudent / drive / status'],['placement_drive_rounds','round_id\norder / published'],['placement_round_results','round_result_id\nresult / recorder']];
  const pos=[[80,170],[80,370],[370,370],[700,170],[700,370],[1010,370],[370,570],[700,570]];
  e.forEach((v,i)=>{shape(s,'roundRect',pos[i][0],pos[i][1],220,88,C.white,C.line,1,{borderRadius:'rounded-lg'});text(s,v[0],pos[i][0]+12,pos[i][1]+10,196,22,{fontSize:16,bold:true,color:C.blue,align:'center'});text(s,v[1],pos[i][0]+14,pos[i][1]+38,192,40,{fontSize:12,color:C.slate,align:'center'});});
  [[190,258,190,370],[190,414,370,414],[300,414,700,414],[810,258,810,370],[920,414,1010,414],[480,458,480,570],[810,458,810,570],[590,614,700,614]].forEach(a=>line(s,...a,C.cyan,2));
  notes(s,'Actual table names include users, departments, students, companies, placement_drives, placement_applications, placement_drive_rounds and placement_round_results. The diagram emphasizes the placement relationships rather than every column.');
}

// 11 Auth
{
  const s = slideBase(p, 'Authentication and user management', 'Security', 14);
  const steps=[['1','Request','Student uses enrollment number. Staff uses university email.'],['2','Verify','BCrypt checks the stored password. Disabled or unsupported accounts fail closed.'],['3','Issue','JWT carries the authenticated identity and role context.'],['4','Reload','Protected services reload the account and use current role/active state.']];
  steps.forEach((x,i)=>{const y=180+i*95; shape(s,'ellipse',70,y,52,52,C.blue,'none',0); text(s,x[0],70,y+13,52,24,{fontSize:20,bold:true,color:C.white,align:'center'}); text(s,x[1],160,y+2,190,25,{fontSize:20,bold:true,color:C.navy}); text(s,x[2],370,y-1,650,48,{fontSize:17,color:C.slate}); if(i<3)line(s,96,y+53,96,y+94,C.cyan,2); });
  box(s,'Current-user contract','/api/me returns safe identity, role, active status and department data when present. Staff accounts do not require a Student row.',850,182,330,170,C.green,{bodySize:15});
  notes(s,'This reflects the implemented compatibility path: student enrollment login is preserved while staff login uses identifier/email and does not require a student profile.');
}

// 12 Drives
{
  const s = slideBase(p, 'Placement drive management', 'Placement workflow', 15);
  const phases=[['Company','Create or reuse a company record'],['Draft','Create drive details and eligibility'],['Publish','Staff publishes only a draft drive'],['Visible','Students see eligible published drives'],['Close','Deadline and status stop new applications']];
  phases.forEach((x,i)=>{const x0=58+i*243; shape(s,'roundRect',x0,220,200,132,i===2?C.blue:C.white,C.line,1,{borderRadius:'rounded-xl',shadow:'shadow-sm'}); text(s,x[0],x0+16,245,168,27,{fontSize:21,bold:true,color:i===2?C.white:C.navy,align:'center'}); text(s,x[1],x0+16,287,168,45,{fontSize:15,color:i===2?'#D7E8F7':C.slate,align:'center'}); if(i<4)line(s,x0+200,286,x0+243,286,C.cyan,3);});
  text(s,'Eligibility uses department, program, graduation year, CGPA and configured backlog rules.',235,470,810,32,{fontSize:21,bold:true,color:C.navy,align:'center'});
  notes(s,'The backend distinguishes DRAFT, PUBLISHED, CLOSED, CANCELLED and ARCHIVED statuses. Students do not see drafts.');
}

// 13 Student workflow
{
  const s = slideBase(p, 'Student application workflow', 'Student experience', 16);
  const steps=[['Browse','GET /api/placement/student/drives'],['Inspect','View company, role, package and eligibility'],['Apply','POST application for an eligible published drive'],['Track','View own applications and progress']];
  steps.forEach((x,i)=>{const x0=80+i*290; shape(s,'roundRect',x0,215,230,170,C.white,C.line,1,{borderRadius:'rounded-xl',shadow:'shadow-sm'}); shape(s,'ellipse',x0+82,188,66,66,[C.cyan,C.blue,C.green,C.amber][i],'none',0); text(s,String(i+1),x0+82,205,66,28,{fontSize:23,bold:true,color:C.white,align:'center'}); text(s,x[0],x0+20,275,190,26,{fontSize:21,bold:true,color:C.navy,align:'center'}); text(s,x[1],x0+18,315,194,50,{fontSize:14,color:C.slate,align:'center'}); if(i<3)line(s,x0+230,300,x0+290,300,C.cyan,3);});
  text(s,'The database unique constraint prevents duplicate applications for the same student and drive.',240,510,800,30,{fontSize:20,bold:true,color:C.navy,align:'center'});
  notes(s,'Student screens are Flutter-based. The backend remains the source of truth for eligibility and duplicate-application protection.');
}

// 14 Rounds
{
  const s = slideBase(p, 'Recruitment round management', 'Recruitment workflow', 17);
  const phases=[['Configure','Create named, ordered rounds\nMark one final round'],['Evaluate','Candidates begin in Pending\nOnly passed students advance'],['Publish','Publish round results\nStudents see published outcomes'],['Decide','Record Selected or Not Selected\nafter a passed final round']];
  phases.forEach((x,i)=>{const x0=72+i*300; shape(s,'roundRect',x0,190,240,205,C.white,C.line,1,{borderRadius:'rounded-xl',shadow:'shadow-sm'}); shape(s,'rect',x0,190,240,8,[C.cyan,C.blue,C.green,C.amber][i],'none',0); text(s,x[0],x0+18,225,204,30,{fontSize:22,bold:true,color:C.navy,align:'center'}); text(s,x[1],x0+20,285,200,66,{fontSize:17,color:C.slate,align:'center'}); if(i<3)line(s,x0+240,292,x0+300,292,C.cyan,3);});
  text(s,'Structural edits are blocked after results exist, protecting the meaning of recorded outcomes.',245,500,790,32,{fontSize:20,bold:true,color:C.navy,align:'center'});
  notes(s,'The current source includes the V5 migration and RecruitmentService. Database application and runtime validation remain a deployment step, not a claim of completion here.');
}

// 15 Dashboards
{
  const s = slideBase(p, 'UTPO and DTPO dashboards', 'Staff operations', 18);
  const table=s.tables.add({rows:5,columns:3,left:64,top:170,width:1152,height:350,values:[['Capability','UTPO','DTPO'],['Companies','Create and list','Create and list within staff authorization'],['Placement drives','University-wide access','Assigned department scope'],['Coordinators','Create and list DTPO accounts','No coordinator administration'],['Round management','Backend endpoints available','Angular controls for department drives']]});
  table.styleOptions={headerRow:true,bandedRows:true}; table.borders.assign({style:'solid',fill:C.line,width:1});
  for(let c=0;c<3;c++){table.getCell(0,c).fill=C.navy;table.getCell(0,c).text.style={typeface:FONT,fontSize:16,bold:true,color:C.white};}
  for(let r=1;r<5;r++)for(let c=0;c<3;c++)table.getCell(r,c).text.style={typeface:FONT,fontSize:15,color:c===0?C.navy:C.slate,bold:c===0};
  text(s,'The dashboard is connected to real APIs for existing placement data. Round UI is present in source; final live verification depends on the V5 schema.',64,575,1110,36,{fontSize:17,color:C.slate});
  notes(s,'Be explicit during review: DTPO scope is enforced server-side. The current branch adds round controls but the migration still needs a controlled database rollout.');
}

// 16 Testing
{
  const s = slideBase(p, 'Testing and verification', 'Quality checks', 19);
  const checks=[['Backend compile','PASS','Java 21 compile and test compilation'],['Focused Flutter tests','PASS','2 widget tests passed'],['Flutter analyze','PASS*','3 style infos only'],['Angular type-check','PASS','ngc application check passed'],['Backend test suite','BLOCKED','Mockito / Byte Buddy agent attachment in local environment'],['Angular production build','BLOCKED','Node process exited 134 in local environment']];
  const table=s.tables.add({rows:7,columns:3,left:64,top:164,width:1152,height:365,values:[['Check','Result','Evidence'],...checks]});
  table.styleOptions={headerRow:true,bandedRows:true}; table.borders.assign({style:'solid',fill:C.line,width:1});
  for(let c=0;c<3;c++){table.getCell(0,c).fill=C.blue;table.getCell(0,c).text.style={typeface:FONT,fontSize:15,bold:true,color:C.white};}
  for(let r=1;r<7;r++){table.getCell(r,1).fill=checks[r-1][1].startsWith('PASS')?'#E8F6F0':'#FFF2DF'; for(let c=0;c<3;c++)table.getCell(r,c).text.style={typeface:FONT,fontSize:14,color:c===1?C.navy:C.slate,bold:c===1};}
  text(s,'No claim of live V5 database verification is made in this deck.',64,575,800,30,{fontSize:18,bold:true,color:C.red});
  notes(s,'Results reflect the current repository checks. The full backend suite is not counted as passed because the environment blocks Mockito’s Byte Buddy agent attachment.');
}

// 17 Limitations
{
  const s = slideBase(p, 'Current limitations and next steps', 'Review honestly', 20);
  box(s,'Pending rollout','Apply and validate V5 in PostgreSQL with a backup and Hibernate ddl-auto=validate.',64,175,350,150,C.amber,{bodySize:16});
  box(s,'Environment checks','Run backend tests in an environment that supports Mockito agent attachment and complete the Angular production build.',465,175,350,150,C.red,{bodySize:16});
  box(s,'Data readiness','Configure approved academic years, departments and placement programs before realistic eligibility tests.',866,175,350,150,C.blue,{bodySize:16});
  text(s,'Future enhancements',64,415,300,28,{fontSize:22,bold:true,color:C.navy});
  text(s,bulletText(['Round-level reporting and richer filters','Operational notifications for students and staff','Broader live integration and deployment verification']),64,462,690,92,{fontSize:18,color:C.slate});
  text(s,'The current V1 avoids inventing outcomes, seed data or performance metrics.',760,440,400,74,{fontSize:21,bold:true,color:C.blue,align:'center'});
  notes(s,'Limitations are based on the current repository state and verification outputs. Separate source presence from live operational proof.');
}

// 18 Conclusion
{
  const s = p.slides.add(); s.background.fill = C.navy;
  shape(s,'rect',0,0,14,H,C.cyan,'none',0); shape(s,'rect',860,0,420,H,'#123C68','none',0);
  text(s,'CAMPUSHIRE',84,106,520,30,{fontSize:16,bold:true,color:'#A9D7F5',letterSpacing:3});
  text(s,'One placement\nworkflow.\nClearer progress.',84,164,650,220,{fontSize:48,bold:true,color:C.white});
  text(s,'A secure foundation for the next stage of university placement operations.',84,430,580,44,{fontSize:21,color:'#D7E8F7'});
  s.images.add({ blob: charusat, contentType: 'image/jpeg', alt: 'CHARUSAT logo', fit: 'contain', position: { left: 902, top: 230, width: 135, height: 135 } });
  s.images.add({ blob: cmpica, contentType: 'image/jpeg', alt: 'CMPICA logo', fit: 'contain', position: { left: 1058, top: 230, width: 135, height: 135 } });
  text(s,'Thank you',902,408,290,40,{fontSize:27,bold:true,color:C.white,align:'center'}); text(s,'Questions and discussion',902,454,290,26,{fontSize:16,color:'#A9D7F5',align:'center'});
  text(s,'21',1168,665,48,20,{fontSize:11,bold:true,color:'#A9D7F5',align:'right'});
  notes(s,'Close by returning to the value: a shared, secure and traceable placement workflow. Invite questions about architecture, authorization and the round model.');
}

for (const [i, slide] of p.slides.items.entries()) {
  const png = await p.export({ slide, format: 'png', scale: 1 });
  await fs.writeFile(path.join(buildDir, `slide-${String(i+1).padStart(2,'0')}.png`), new Uint8Array(await png.arrayBuffer()));
}
await fs.mkdir(path.dirname(finalPath), { recursive: true });
const pptx = await PresentationFile.exportPptx(p);
await pptx.save(candidatePath);
console.log(JSON.stringify({candidatePath, slideCount:p.slides.items.length, width:W, height:H}));
