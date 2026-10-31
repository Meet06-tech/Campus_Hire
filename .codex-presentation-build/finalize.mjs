import path from 'node:path';
import fs from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

const skillDir = '/Users/meetbhoi/.codex/plugins/cache/openai-primary-runtime/presentations/26.1022.11436/skills/presentations';
const workspaceDir = '/Users/meetbhoi/Downloads/V1/placement-management-system';
const candidatePath = path.join(workspaceDir, '.codex-presentation-build', 'CampusHire_Project_Presentation_With_Diagrams_candidate.pptx');
const finalPath = path.join(workspaceDir, 'presentations', 'CampusHire_Project_Presentation_With_Diagrams.pptx');
const stagingDir = path.join(workspaceDir, '.codex-presentation-build', 'finalizer');
await fs.mkdir(stagingDir, { recursive: true });
const { finalizePresentation } = await import(pathToFileURL(path.join(skillDir, 'container_tools/artifact_tool_utils.mjs')).href);
const result = await finalizePresentation({
  explicitTotalSlideCount: 21,
  requiredNativeTableOwnerSlides: [5, 6, 18, 19],
  candidatePath,
  finalPath,
  workspaceDir,
  pythonExecutable: '/Users/meetbhoi/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3',
  integrityValidatorPath: path.join(skillDir, 'container_tools/inspect_presentation_package_integrity.py'),
  layoutValidatorPath: path.join(skillDir, 'container_tools/inspect_presentation_layout_geometry.py'),
  layoutArgs: ['--expected-slide-size-emu', '12192000,6858000', '--validate-bullet-geometry', '--validate-heading-fit', '--require-native-table-slide', '5', '--require-native-table-slide', '6', '--require-native-table-slide', '18', '--require-native-table-slide', '19'],
  fontPolicy: { basis: 'design', families: ['Arial'] },
  verifyArtifactToolImport: true,
  receiptPath: path.join(stagingDir, 'CampusHire_Project_Presentation_With_Diagrams.validation.json'),
});
console.log(JSON.stringify(result));
