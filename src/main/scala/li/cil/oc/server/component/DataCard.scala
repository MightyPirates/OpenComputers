package li.cil.oc.server.component

import java.security._
import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.zip.{DeflaterOutputStream, InflaterOutputStream}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher, KeyAgreement, Mac}

import com.google.common.hash.Hashing
import li.cil.oc.{OpenComputers, Settings, api}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.api.{Network, prefab}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.output.ByteArrayOutputStream

class DataCard extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("data", Visibility.Neighbors).
    withConnector().
    create()

  val romData = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/data"), "data"))

  protected def checkCost(context: Context, args: Arguments, baseCost: Double, byteCost: Double): Array[Byte] = {
    val data = args.checkByteArray(0)
    if (data.length > Settings.get.dataCardHardLimit) throw new IllegalArgumentException("data size limit exceeded")
    if (!node.tryChangeBuffer(-baseCost - data.length * byteCost)) throw new Exception("not enough energy")
    if (data.length > Settings.get.dataCardSoftLimit) context.pause(Settings.get.dataCardTimeout)
    data
  }

  protected def checkCost(baseCost: Double): Unit = {
    if (!node.tryChangeBuffer(-baseCost)) throw new Exception("not enough energy")
  }

  protected def trivialCost(context: Context, args: Arguments) =
    checkCost(context, args, Settings.get.dataCardTrivial, Settings.get.dataCardTrivialByte)

  protected def simpleCost(context: Context, args: Arguments) =
    checkCost(context, args, Settings.get.dataCardSimple, Settings.get.dataCardSimpleByte)

  protected def complexCost(context: Context, args: Arguments) =
    checkCost(context, args, Settings.get.dataCardComplex, Settings.get.dataCardComplexByte)

  protected def asymmetricCost(context: Context, args: Arguments) =
    checkCost(context, args, Settings.get.dataCardAsymmetric, Settings.get.dataCardComplexByte)

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node.isNeighborOf(this.node)) {
      romData.foreach(fs => node.connect(fs.node))
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      romData.foreach(_.node.remove())
    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    romData.foreach(_.load(nbt.getCompoundTag("romData")))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    romData.foreach(fs => nbt.setNewCompoundTag("romData", fs.save))
  }
}

object DataCard {
  val SecureRandomInstance = new ThreadLocal[SecureRandom]() {
    override def initialValue = SecureRandom.getInstance("SHA1PRNG")
  }

  class Tier1 extends DataCard {
    @Callback(direct = true, doc = """function():number -- The maximum size of data that can be passed to other functions of the card.""")
    def getLimit(context: Context, args: Arguments): Array[AnyRef] = {
      result(Settings.get.dataCardHardLimit)
    }

    @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Applies base64 encoding to the data.""")
    def encode64(context: Context, args: Arguments): Array[AnyRef] = {
      result(Base64.encodeBase64(trivialCost(context, args)))
    }

    @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Applies base64 decoding to the data.""")
    def decode64(context: Context, args: Arguments): Array[AnyRef] = {
      result(Base64.decodeBase64(trivialCost(context, args)))
    }

    @Callback(direct = true, limit = 4, doc = """function(data:string):string -- Applies deflate compression to the data.""")
    def deflate(context: Context, args: Arguments): Array[AnyRef] = {
      val data = complexCost(context, args)
      val baos = new ByteArrayOutputStream(512)
      val deos = new DeflaterOutputStream(baos)
      deos.write(data)
      deos.finish()
      result(baos.toByteArray)
    }

    @Callback(direct = true, limit = 4, doc = """function(data:string):string -- Applies inflate decompression to the data.""")
    def inflate(context: Context, args: Arguments): Array[AnyRef] = {
      val data = complexCost(context, args)
      val baos = new ByteArrayOutputStream(512)
      val inos = new InflaterOutputStream(baos)
      inos.write(data)
      inos.finish()
      result(baos.toByteArray)
    }

    @Callback(direct = true, limit = 4, doc = """function(data:string):string -- Computes SHA2-256 hash of the data. Result is in binary format.""")
    def sha256(context: Context, args: Arguments): Array[AnyRef] = {
      val data = complexCost(context, args)
      result(Hashing.sha256().hashBytes(data).asBytes())
    }

    @Callback(direct = true, limit = 8, doc = """function(data:string):string -- Computes MD5 hash of the data. Result is in binary format""")
    def md5(context: Context, args: Arguments): Array[AnyRef] = {
      val data = simpleCost(context, args)
      result(Hashing.md5().hashBytes(data).asBytes())
    }

    @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Computes CRC-32 hash of the data. Result is in binary format""")
    def crc32(context: Context, args: Arguments): Array[AnyRef] = {
      val data = trivialCost(context, args)
      result(Hashing.crc32().hashBytes(data).asBytes())
    }

    @Callback(direct = true, limit = 32, doc = """function():number -- Returns a tier of the card""")
    def tier(context: Context, args: Arguments): Array[AnyRef] = result(1)
  }

  class Tier2 extends Tier1 {
    @Callback(direct = true, limit = 32, doc = """function():number -- Returns a tier of the card""")
    override def tier(context: Context, args: Arguments): Array[AnyRef] = result(2)

    private def crypt(context: Context, args: Arguments, mode: Int): Array[AnyRef] = {
      val data = simpleCost(context, args)

      val key = args.checkByteArray(1)
      if (key.length != 16)
        throw new IllegalArgumentException("Expected a 128-bit AES key")

      val iv = args.checkByteArray(2)
      if (iv.length != 16)
        throw new IllegalArgumentException("Expected a 128-bit AES IV")

      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv))
      result(cipher.doFinal(data))
    }

    @Callback(direct = true, limit = 8, doc = """function(data:string, key: string, iv:string):string -- Encrypt data with AES. Result is in binary format.""")
    def encrypt(context: Context, args: Arguments): Array[AnyRef] = crypt(context, args, Cipher.ENCRYPT_MODE)

    @Callback(direct = true, limit = 8, doc = """function(data:string, key:string, iv:string):string -- Decrypt data with AES""")
    def decrypt(context: Context, args: Arguments): Array[AnyRef] = crypt(context, args, Cipher.DECRYPT_MODE)

    private def hash(context: Context, args: Arguments, mode: String, hmacMode: String, simple: Boolean = false): Array[AnyRef] = {
      val data = if (simple) simpleCost(context, args) else complexCost(context, args)

      if (args.count() > 1) {
        val key = args.checkByteArray(1)

        val hmac = Mac.getInstance(hmacMode)
        hmac.init(new SecretKeySpec(key, hmacMode))
        result(hmac.doFinal(data))
      } else {
        result(MessageDigest.getInstance(mode).digest(data))
      }
    }

    @Callback(direct = true, limit = 4, doc = """function(data:string[, hmacKey:string]):string -- Computes SHA2-256 hash of the data. Result is in binary format.""")
    override def sha256(context: Context, args: Arguments): Array[AnyRef] = hash(context, args, "SHA-256", "HmacSHA256")

    @Callback(direct = true, limit = 8, doc = """function(data:string[, hmacKey:string]):string -- Computes MD5 hash of the data. Result is in binary format""")
    override def md5(context: Context, args: Arguments): Array[AnyRef] = hash(context, args, "MD5", "HmacMD5", simple = true)

    @Callback(direct = true, limit = 4, doc = """function(len:number):string -- Generates secure random binary data""")
    def random(context: Context, args: Arguments): Array[AnyRef] = {
      checkCost(Settings.get.dataCardComplex)
      val len = args.checkInteger(0)

      if (len <= 0 || len > 1024)
        throw new IllegalArgumentException("Length must be in range [1..1024]")

      val target = new Array[Byte](len)
      SecureRandomInstance.get.nextBytes(target)
      result(target)
    }
  }

  object ECUserdata {
    def deserializeKey(t: String, data: Array[Byte]): Key = {
      val fact = KeyFactory.getInstance("EC")

      t match {
        case "ec-private" => fact.generatePrivate(new PKCS8EncodedKeySpec(data))
        case "ec-public" => fact.generatePublic(new X509EncodedKeySpec(data))
        case _ => throw new IllegalArgumentException("Wrong key type. Currently supported: ec-public, ec-private")
      }
    }
  }

  class ECUserdata extends prefab.AbstractValue {
    var k: Key = null

    // Hack to keep empty constructor for deserialization
    def this(_k: Key) = {
      this()
      k = _k
    }

    private def keyType = k match {
      case x: ECPrivateKey => "ec-private"
      case x: ECPublicKey => "ec-public"
    }

    @Callback(direct = true, limit = 32, doc = "function():string -- Returns type of key")
    def keyType(context: Context, args: Arguments): Array[AnyRef] = result(keyType)

    @Callback(direct = true, limit = 4, doc = "function():string -- Returns string representation of key. Result is in binary format.")
    def serialize(context: Context, args: Arguments): Array[AnyRef] = result(k.getEncoded)

    @Callback(direct = true, limit = 32, doc = "function():boolean -- Returns whether key is public")
    def isPublic(context: Context, args: Arguments): Array[AnyRef] = result(isPublic)

    def isPublic = k.isInstanceOf[ECPublicKey]

    override def load(nbt: NBTTagCompound): Unit =
      k = ECUserdata.deserializeKey(nbt.getString("Type"), nbt.getByteArray("Data"))

    override def save(nbt: NBTTagCompound): Unit = {
      nbt.setString("Type", keyType)
      nbt.setByteArray("Data", k.getEncoded)
    }
  }

  class Tier3 extends Tier2 {
    @Callback(direct = true, limit = 32, doc = """function():number -- Returns a tier of the card""")
    override def tier(context: Context, args: Arguments): Array[AnyRef] = result(3)

    @Callback(direct = true, limit = 1, doc = """function([bitLen:number]):eckey,eckey -- Generates key pair. Returns: public, private keys. Allowed key lengths: 256, 384 bits""")
    def generateKeyPair(context: Context, args: Arguments): Array[AnyRef] = {
      checkCost(Settings.get.dataCardAsymmetric)
      var bitLen = 384

      if (args.count() > 0) {
        bitLen = args.checkInteger(0)

        if (bitLen != 256 && bitLen != 384)
          throw new IllegalArgumentException("Invalid key length. Allowed: 256, 384")
      }

      val kpg = KeyPairGenerator.getInstance("EC")
      kpg.initialize(bitLen, SecureRandomInstance.get)
      val kp = kpg.generateKeyPair()

      result(new ECUserdata(kp.getPublic), new ECUserdata(kp.getPrivate))
    }

    @Callback(direct = true, limit = 8, doc = """function(data:string, type:string):eckey -- Restores key from its string representation.""")
    def deserializeKey(context: Context, args: Arguments): Array[AnyRef] = {
      val data = simpleCost(context, args)
      val t = args.checkString(1)

      result(new ECUserdata(ECUserdata.deserializeKey(t, data)))
    }

    private def checkUserdata(args: Arguments, i: Int, isPublic: Boolean = false, anyAccepted: Boolean = false) =
      args.checkAny(i) match {
        case x: ECUserdata =>
          if (anyAccepted || x.isPublic == isPublic) x
          else throw new IllegalArgumentException((if (isPublic) "Public" else "Private") + " key expected at " + i)
        case x => throw new IllegalArgumentException("Userdata expected at " + i)
      }

    @Callback(direct = true, limit = 1, doc = """function(priv:eckey, pub:eckey):string -- Generates a shared key. ecdh(a.priv, b.pub) == ecdh(b.priv, a.pub)""")
    def ecdh(context: Context, args: Arguments): Array[AnyRef] = {
      checkCost(Settings.get.dataCardAsymmetric)
      val privKey = checkUserdata(args, 0, isPublic = false).k
      val pubKey = checkUserdata(args, 1, isPublic = true).k

      val ka = KeyAgreement.getInstance("ECDH")
      ka.init(privKey)
      ka.doPhase(pubKey, true)
      result(ka.generateSecret)
    }

    @Callback(direct = true, limit = 1, doc = """function(data:string, key:eckey[, sig:string]):string|boolean -- Signs or verifies data""")
    def ecdsa(context: Context, args: Arguments): Array[AnyRef] = {
      val data = asymmetricCost(context, args)
      val key = checkUserdata(args, 1, anyAccepted = true)
      val sig = args.optByteArray(2, null)

      val sign = Signature.getInstance("SHA256withECDSA")
      if (sig != null) {
        // Verify mode
        if (!key.isPublic)
          throw new IllegalArgumentException("Public key expected")

        sign.initVerify(key.k.asInstanceOf[PublicKey])
        sign.update(data)
        result(sign.verify(sig))
      } else {
        // Sign mode
        if (key.isPublic)
          throw new IllegalArgumentException("Private key expected")

        sign.initSign(key.k.asInstanceOf[PrivateKey])
        sign.update(data)
        result(sign.sign())
      }
    }
  }
}