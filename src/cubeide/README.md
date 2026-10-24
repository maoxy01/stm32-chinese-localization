# CubeIDE 补充资源

`ResourceOverlay.java` 从用户本机已合法安装的 STM32CubeIDE 插件读取 `.properties` 资源，并依据 `ide-extra-zh.tsv` 生成只包含 `_zh_CN.properties` 的 OSGi fragment。它不会修改或复制原厂 `.class` 文件。

`dist/cubeide-supplement-repository` 是针对已验证 host bundle 版本生成并通过回归检查的当前产物。升级 CubeIDE 后应重新提取本机资源、构建并测试。

