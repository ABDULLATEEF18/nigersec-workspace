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
                import asyncio as _asyncio
                end = _asyncio.get_event_loop().time() + 20
                while _asyncio.get_event_loop().time() < end:
                    try:
                        msg = await _asyncio.wait_for(ws.recv(), timeout=20)
                        print('message:', msg)
                    except _asyncio.TimeoutError:
                        break
            except Exception as e:
                print('recv error:', e)
    except Exception as e:
        print('connect error:', e)

if __name__ == '__main__':
    asyncio.run(listen())
