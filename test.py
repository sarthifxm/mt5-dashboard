import MetaTrader5 as mt5

MT5_PATH = r"C:\Program Files\MetaTrader\terminal64.exe"

connected = mt5.initialize(path=MT5_PATH)

print("CONNECTED:", connected)
print("VERSION:", mt5.version())
print("ERROR:", mt5.last_error())