# 第三方来源与许可证

## STM32CubeMX-Chinese-Patcher

- 上游：<https://github.com/fraternity-z/STM32CubeMX-Chinese-Patcher>
- 本机验证时使用的版本：v1.0.5。
- 上游仓库在核对时没有可识别的 `LICENSE` 文件。因此本仓库不复制其源代码、可执行文件或原始词典，只提供本项目新增的词条以及合并脚本。使用者应从上游自行取得并遵守其条款。

## Eclipse Babel

- 项目：<https://projects.eclipse.org/projects/technology.babel>
- 官方归档：<https://archive.eclipse.org/technology/babel/babel_language_packs/R0.20.0/2022-12/>
- 项目许可证：Eclipse Public License 1.0。

本仓库不镜像 Babel 语言包；基础包应从 Eclipse 官方归档获取。

## Eclipse SWT issue 3752

- 上游问题：<https://github.com/eclipse-platform/eclipse.platform.swt/issues/3752>

本仓库的 Java Agent 是针对本机确认版本的独立运行时规避方案，不是 Eclipse 或 ST 的官方修复。它只在目标 SWT JAR、`Table.class` 哈希和预期字节码结构同时匹配时生效。

## ASM 9.9

预构建 Java Agent 包含来自 STM32CubeIDE 安装中的 ASM 9.9 类。ASM 使用 BSD 3-Clause License；完整许可证随包保存在 `dist/swt-tooltip-fix/NOTICE.txt`。

## STMicroelectronics 软件与资源

STM32CubeIDE / STM32CubeMX 由 STMicroelectronics 提供，并包含多种第三方组件和许可证。本仓库不包含完整 ST 软件或原厂程序 JAR。资源补充只用于已合法安装的 STM32Cube 软件，使用者仍须遵守对应版本的 ST 附加许可条款：

<https://www.st.com/content/ccc/resource/legal/legal_agreement/additional_license_terms/group1/4c/c0/96/2d/e9/b4/48/e7/additional-license-terms-for-stm32cubeide-v2-2-0/files/additional-license-terms-for-stm32cubeide-v2-2-0.html/jcr%3Acontent/translations/en.additional-license-terms-for-stm32cubeide-v2-2-0.html>

本仓库未对第三方内容重新授权，也不使用 ST、Eclipse 或上游作者的名称为本项目背书。

## STM32CubeProgrammer 与 Trusted Package Creator

STM32CubeProgrammer 2.23 已由 ST 提供官方中文界面资源，因此本仓库不复制 `translations_ch.qm`。对套件内尚未完整中文化的 Trusted Package Creator，本仓库只提供词典和版本锁定构建器；不提交修改后的 ST 可执行文件。构建器要求用户本机已有合法安装，并且仅对已验证原版哈希执行等长文本替换。

