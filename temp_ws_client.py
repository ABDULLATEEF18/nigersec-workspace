import asyncio

async def listen():
    try:
        import websockets
    except Exception as e:
        print('websockets package missing:', e)
        return
    uri = 'ws://localhost:8080/api/v1/ws/fraud'
    try:
        async with websockets.connect(uri) as ws:
            print('connected')
            try:
                msg = await ws.recv()
                print('message:', msg)
            except Exception as e:
                print('recv error:', e)
    except Exception as e:
        print('connect error:', e)

if __name__ == '__main__':
    asyncio.run(listen())
