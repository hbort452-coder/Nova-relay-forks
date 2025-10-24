import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun main() {
    println("=== NovaRelay Logs Test ===")
    
    // Создаем тестовые логи
    val logs = mutableListOf<String>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    
    logs.add("=== Session start ${dateFormat.format(Date())} ===")
    logs.add("[${dateFormat.format(Date())}] C->S RequestNetworkSettingsPacket RequestNetworkSettingsPacket(protocolVersion=800)")
    logs.add("[${dateFormat.format(Date())}] Fetched bedrock codec: 786 for protocol: 800")
    logs.add("[${dateFormat.format(Date())}] Sent NetworkSettings(ZLIB, threshold=0) and enabled server compression")
    logs.add("[${dateFormat.format(Date())}] C->S LoginPacket LoginPacket(protocolVersion=800, authPayload=...)")
    logs.add("[${dateFormat.format(Date())}] S->C NetworkSettingsPacket from server - BLOCKED (relay already sent its own)")
    logs.add("[${dateFormat.format(Date())}] Sending LoginPacket with protocol version: 786")
    logs.add("[${dateFormat.format(Date())}] Login success")
    logs.add("[${dateFormat.format(Date())}] S->C StartGamePacket StartGamePacket(...)")
    logs.add("[${dateFormat.format(Date())}] Start game, setting definitions")
    logs.add("[${dateFormat.format(Date())}] S->C ResourcePackStackPacket ResourcePackStackPacket(...)")
    logs.add("[${dateFormat.format(Date())}] S->C ResourcePackDataInfoPacket ResourcePackDataInfoPacket(...)")
    logs.add("[${dateFormat.format(Date())}] S->C ResourcePackChunkDataPacket ResourcePackChunkDataPacket(...)")
    logs.add("[${dateFormat.format(Date())}] Client connected successfully!")
    logs.add("=== Session end ${dateFormat.format(Date())} ===")
    
    // Сохраняем логи в файл
    val logFile = File("test_logs.txt")
    logFile.writeText(logs.joinToString("\n"))
    
    println("✅ Тестовые логи созданы: ${logFile.absolutePath}")
    println("📊 Всего записей: ${logs.size}")
    println("📁 Размер файла: ${logFile.length()} байт")
    
    // Показываем первые несколько строк
    println("\n📝 Первые 5 строк логов:")
    logs.take(5).forEach { println("  $it") }
    
    println("\n🎯 Теперь можно:")
    println("  1. Скопировать логи: cat test_logs.txt | xclip -selection clipboard")
    println("  2. Очистить логи: rm test_logs.txt")
    println("  3. Просмотреть логи: cat test_logs.txt")
}