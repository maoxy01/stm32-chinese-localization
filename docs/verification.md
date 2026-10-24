# 验证范围

验证日期：2026-10-23。平台：Windows x64。

## 自动检查

- CubeMX 词典共验证 467 条大小写敏感记录；本仓库新增部分包含 132 条记录（其中 3 组仅大小写不同）。
- 45 项 UI 汉化回归检查通过，包括：可编辑文本不被翻译、下拉框数据模型不变、芯片型号不变、数字列排序、按钮/菜单 action command 不变、重复翻译不累计监听器。
- CubeIDE 11 个补充 fragment 均为纯资源包，不包含 `.class` 文件；共 373 条翻译。
- 固定 IOC 在启用和禁用汉化时生成的 6 个 `.c` / `.h` 文件 SHA-256 全部一致。
- CubeIDE 自带 `arm-none-eabi-gcc` 的最小编译链接测试通过。

## SWT 修复验证

- 对应上游 Eclipse SWT issue 3752：Windows 命中测试可能返回有效行号和 `iSubItem == -1`，原路径随后访问 `cellFont[-1]`。
- 48 个边界用例通过，覆盖中文/英文 Locale、0/1/5/22 个显式列、负数、首列、末列、上边界和极值。
- 修改一字节后的目标 class 会被版本保护拒绝。
- 中文与英文实际界面均加载 3257 项器件并进入工程设置页；测试会话的 `ArrayIndexOutOfBoundsException`、`No more handles`、`Unhandled event loop exception` 计数均为 0。
- 原厂 SWT JAR SHA-256 保持：`E7D7325C06B6EC5820D669E1F66515CCA88A17F9522EF1E8BD1DE65FF69EEF9B`。
- Agent SHA-256：`6B8D7CBBCE2A307343453EF4585C0FA7EE8B82E111677F2DB9A78BDF7D0C378F`。

## 已知限制

- CubeMX 内嵌网页、动态计数、部分新文案仍可能显示英文。
- CubeIDE 的硬编码标题和未覆盖的新模块仍可能显示英文。
- 未进行完整 HAL 工程构建、真实开发板烧录、硬件调试、外设时序或长期稳定性测试。
- 任何 STM32CubeIDE / SWT 升级都需要重新核对哈希和字节码结构；不得仅修改版本号来绕过保护。

## Trusted Package Creator

- 原版来自 STM32CubeProgrammer 2.23.0，文件版本 2.9.0.0，SHA-256 为 `017305908641977B62DD786BD08F7687F19A2ADEB7DEB01843E6DC546A2607DD`。
- 中文副本只在已审查的 GUI 字符串池内进行 304 处等长 UTF-8 替换；文件长度、`.text`、导入表和其他 PE 节未修改。
- 已检查主界面以及 SFI、SSP、WB 签名页面切换。
- 未连接硬件，未执行 HSM 烧录、锁定、密钥生成或真实安全固件安装。中文副本的原厂数字签名因内容变化不再有效，因此本仓库不分发该 EXE，只提供可审计的本机构建器。

