# STM32 中文本地化补充包

面向 Windows x64 的非官方 STM32 简体中文补充包，覆盖三款已处理软件：STM32CubeMX、STM32CubeIDE，以及 STM32CubeProgrammer 套件内的 STM32 Trusted Package Creator。CubeIDE 补充部分以纯资源 OSGi fragment 安装，已知 SWT 问题通过版本锁定的 Java Agent 在运行时规避；Trusted Package Creator 由作者本机的原版 EXE 生成中文副本，仓库不分发 ST 的可执行文件。

> 本项目不是 STMicroelectronics、Eclipse Foundation 或 CubeMX 汉化工具作者的官方发布。请先阅读 [第三方来源与许可证](THIRD_PARTY_NOTICES.md)。

## 已验证版本

| 组件 | 已验证版本 | 内容 |
| --- | --- | --- |
| STM32CubeMX | 6.18.1-RC2 | 在现有 `STM32CubeMX-Chinese-Patcher` 词典上追加 131 条中文词条 |
| STM32CubeIDE | 2.2.0 / Windows x64 | 11 个 ST 专属界面的纯资源中文 fragment，共 373 条翻译 |
| Eclipse SWT | `3.132.0.v20251124-0642` | 修复器件选择器 tooltip 无效列索引路径；严格校验原 JAR SHA-256 |
| STM32 Trusted Package Creator | 随 STM32CubeProgrammer 2.23.0 安装；EXE 文件版本 2.9.0.0 | 从本机原版生成主要界面中文副本，304 处等长 UTF-8 文本替换 |

版本不一致时脚本会停止，而不是尝试“强行兼容”。STM32CubeIDE 或 SWT 升级后必须重新验证。

## 仓库结构

- `dist/cubeide-supplement-repository/`：可由 p2 director 安装的 11 个资源 fragment。
- `dist/swt-tooltip-fix/`：已测试 Java Agent 与开源 ASM 的许可证告知。
- `src/cubemx/cubemx-extra-zh.tsv`：仅包含本项目新增的 CubeMX 词条，不包含第三方汉化工具原始词典。
- `src/cubeide/`：补充词典、资源构建器与回归测试源码。
- `src/swt-tooltip-fix/`：SWT 修复源码和可重复构建脚本。
- `src/trusted-package-creator/`：Trusted Package Creator 中文词典与版本锁定构建器。
- `scripts/`：安装、回退和完整性检查脚本。
- `docs/verification.md`：验证范围、已知限制与哈希。

## 安装

### 1. 基础汉化

1. CubeMX：从上游项目获取并安装 [`fraternity-z/STM32CubeMX-Chinese-Patcher`](https://github.com/fraternity-z/STM32CubeMX-Chinese-Patcher)。本仓库不镜像其 EXE 或原始词典。
2. CubeIDE：从 [Eclipse Babel 0.20.0 / 2022-12 官方归档](https://archive.eclipse.org/technology/babel/babel_language_packs/R0.20.0/2022-12/) 获取 Eclipse 与 CDT 简体中文语言包，并按 p2 方式安装；在 `stm32cubeide.ini` 的 `-vmargs` 前设置 `-nl` 和 `zh_CN`。

### 2. 安装本项目补充

关闭 STM32CubeMX 和 STM32CubeIDE，以 PowerShell 运行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
& .\scripts\install-cubemx-extra.ps1 -CubeMxRoot 'E:\STMicroelectronics\STM32CubeMX'
& .\scripts\install-cubeide-supplement.ps1 -CubeIdeRoot 'E:\STMicroelectronics\STM32CubeIDE'
& .\scripts\install-swt-tooltip-fix.ps1 -CubeIdeRoot 'E:\STMicroelectronics\STM32CubeIDE'
& .\scripts\build-trusted-package-creator.ps1 -CubeProgrammerRoot 'E:\STMicroelectronics\STM32CubeProgrammer' -Python 'C:\Python312\python.exe'
```

每个脚本都会检查目标、保留回退记录并在不匹配时停止。不要在软件运行时安装。

### 3. 回退

```powershell
& .\scripts\install-swt-tooltip-fix.ps1 -CubeIdeRoot 'E:\STMicroelectronics\STM32CubeIDE' -Remove
& .\scripts\install-cubeide-supplement.ps1 -CubeIdeRoot 'E:\STMicroelectronics\STM32CubeIDE' -Remove
& .\scripts\install-cubemx-extra.ps1 -CubeMxRoot 'E:\STMicroelectronics\STM32CubeMX' -Remove
```

CubeMX 回退会校验当前词典仍是脚本安装后的版本；如用户之后又修改过词典，脚本会拒绝覆盖并要求人工检查。

Trusted Package Creator 构建脚本不会覆盖原版，只在同一 `bin` 目录生成 `STM32TrustedPackageCreator_zh_CN.exe` 和清单。删除中文副本即可回退；原版的 ST 数字签名保持有效，中文副本因内容变化不再具有有效的原厂签名。

## 验证结论

- 45 项汉化回归检查通过。
- 同一固定 IOC 在启用/禁用汉化时生成的 6 个 C/H 文件 SHA-256 一致。
- CubeIDE 中英文器件选择器均加载 3257 项并可进入下一步，修复会话未出现新的数组越界、句柄耗尽或未处理事件循环错误。
- 原厂 SWT JAR 未修改。
- Trusted Package Creator 原版 EXE 保持不变；版本锁定构建器仅接受 SHA-256 `017305908641977B62DD786BD08F7687F19A2ADEB7DEB01843E6DC546A2607DD`，生成副本 SHA-256 为 `87698B2FE57C7B2CFF49904907A19D79EC40E66133CC1C5F2C5E73C8B4070CC3`。

这些结果不等于完整 HAL 工程、开发板烧录、硬件调试或所有 STM32Cube 功能均已验收。详细范围见 [docs/verification.md](docs/verification.md)。

