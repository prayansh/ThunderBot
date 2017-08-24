import pyvjoy
import time

#Pythonic API, item-at-a-time

p1 = pyvjoy.VJoyDevice(1)
p2 = pyvjoy.VJoyDevice(2)

def resetDevice(player):
    player.data.wAxisX = 16383
    player.data.wAxisY = 16383
    player.data.wAxisYRot = 16383
    player.data.wAxisXRot = 16383
    player.data.wAxisZ = 0
    player.data.wAxisZRot = 0
    player.data.lButtons = 0

    #send data to vJoy device
    player.update()


def resetAll():
    resetDevice(p1)
    resetDevice(p2)

resetAll()