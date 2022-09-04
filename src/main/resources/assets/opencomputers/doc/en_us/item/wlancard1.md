# WLAN Card

![May cause cancer. May not.](oredict:opencomputers:wlanCard2)

The wireless network card is an upgraded [network card](lanCard.md) that can send and receive wireless network messages. Tier two cards can also send and receive wired messages. The signal strength directly controls the distance up to which a sent message can be received, where the strength is equal to the distance in blocks.

The higher the signal strength, the more energy it will take to send a single message. The terrain between the sender and receiver also determines whether a message will be successfully transmitted or not. To penetrate a block, the blocks hardness is subtracted from the signal strength - with the minimum being one for air blocks. If no strength remains to reach the receiver, the message will not be received. This is not an exact science however - sometimes messages may still reach the target. In general you will want to make sure the line of sight between the sender and receiver is clear.
