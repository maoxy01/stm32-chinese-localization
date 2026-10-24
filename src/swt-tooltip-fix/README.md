# SWT tooltip 修复源码

该 Java Agent 针对 Eclipse SWT issue 3752 的已确认路径，并通过目标 JAR 哈希、`Table.class` 哈希、方法描述符和字节码结构限制适用范围。

```powershell
& .\build.ps1 -CubeIdeRoot 'E:\STMicroelectronics\STM32CubeIDE' -JavaHome 'D:\JDK17'
```

重新构建后的 JAR 必须再次执行边界、版本拒绝和中英文 GUI 回归测试，不能仅更新固定哈希后直接发布。

