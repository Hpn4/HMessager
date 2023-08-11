import asyncio
import websockets

from binascii import hexlify
from time import sleep
import os

MAC_SIZE = 64
PORT = 8765

# Opened websockets
# key: pub keys
# value: [websockets, busy]
cons = {}

async def handler(websocket):
	# Firslty we will get the hashed public key
    pubkey = await websocket.recv()
    pubkey = hexlify(pubkey)

    print(f"Hashed identity <<< {pubkey}")

    # We register the websockets and mark it as busy (the server will send saved messages)
    cons[pubkey] = [websocket, True]

    await sendSavedMsg(websocket, pubkey)

    cons[pubkey] = [websocket, False]

    while True:
    	try:
            msg = await websocket.recv()
            await redirectMsg(msg)
    	except websockets.ConnectionClosed as e:
    		print(f"Connection with: {pubkey} closing, error: {repr(e)}")
    		await websocket.close()
    		break

    print(f"Connection with: {pubkey} closed")

    del cons[pubkey]

async def sendSavedMsg(websocket, pubkey):
    fileName = str(pubkey, 'utf-8')

    if not os.path.exists(fileName):
        return

    fd = open(fileName, "rb+"); # Read as binary

    while True:
        bSize = fd.read(4) # Read 4 bytes

        if len(bSize) == 0: # EOF
            break

        size = int.from_bytes(bSize, byteorder='big', signed=False)

        msg = fd.read(size)

        await websocket.send(msg)

    fd.truncate(0)
    fd.close()


async def redirectMsg(msg):
    dstPubKey = hexlify(msg[64:64 + 32]) # Get the dest user

    print(dstPubKey)

    # If the dest user is connected, we send to it the message
    if dstPubKey in cons:
        while cons[dstPubKey][1]: # Wait until not busy
            sleep(0.1)

        await cons[dstPubKey][0].send(msg)
    else:

        # Otherwise we write in a file the message
        fileName = str(dstPubKey, 'utf-8')
        fd = open(fileName, "ab+")

        n = len(msg)
        fd.write(n.to_bytes(4, 'big')) # Write the size of the message
        fd.write(msg) # Write the message

        fd.close()

async def main():
    async with websockets.serve(handler, "localhost", PORT):
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    asyncio.run(main())
