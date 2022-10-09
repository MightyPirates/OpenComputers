package li.cil.oc.server.component

import java.security._
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

import com.google.common.hash.Hashing
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import net.minecraft.nbt.NBTTagCompound
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.output.ByteArrayOutputStream

import scala.collection.convert.WrapAsJava._

abstract class DataCard extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("data", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  protected def checkCost(context: Context, args: Arguments, baseCost: Double, byteCost: Double): Array[Byte] = {
    val data = args.checkByteArray(0)
    if (data.length > Settings.get.dataCardHardLimit) throw new IllegalArgumentException("data size limit exceeded")
    val cost = baseCost + data.length * byteCost
    if (!node.tryChangeBuffer(-cost)) throw new Exception("not enough energy")
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

  @Callback(direct = true, doc = """function():number -- The maximum size of data that can be passed to other functions of the card.""")
  def getLimit(context: Context, args: Arguments): Array[AnyRef] = {
    result(Settings.get.dataCardHardLimit)
  }
}

object DataCard {
  val SecureRandomInstance = new ThreadLocal[SecureRandom]() {
    override def initialValue = SecureRandom.getInstance("SHA1PRNG")
  }

  class Tier1 extends DataCard {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Processor,
      DeviceAttribute.Description -> "Data processor card",
      DeviceAttribute.Vendor -> "S.C. Ltd.",
      DeviceAttribute.Product -> "SC01D H45h3r"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo

    // ----------------------------------------------------------------------- //

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

    @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Computes CRC-32 hash of the data. Result is binary data.""")
    def crc32(context: Context, args: Arguments): Array[AnyRef] = {
      val data = trivialCost(context, args)
      result(Hashing.crc32().hashBytes(data).asBytes())
    }

    @Callback(direct = true, limit = 8, doc = """function(data:string):string -- Computes MD5 hash of the data. Result is binary data.""")
    def md5(context: Context, args: Arguments): Array[AnyRef] = {
      val data = simpleCost(context, args)
      result(Hashing.md5().hashBytes(data).asBytes())
    }

    @Callback(direct = true, limit = 4, doc = """function(data:string):string -- Computes SHA2-256 hash of the data. Result is binary data.""")
    def sha256(context: Context, args: Arguments): Array[AnyRef] = {
      val data = complexCost(context, args)
      result(Hashing.sha256().hashBytes(data).asBytes())
    }
  }

  class Tier2 extends Tier1 {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Processor,
      DeviceAttribute.Description -> "Data processor card",
      DeviceAttribute.Vendor -> "S.C. Ltd.",
      DeviceAttribute.Product -> "SC02D Cryptic"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, limit = 8, doc = """function(data:string[, hmacKey:string]):string -- Computes MD5 hash of the data. Result is binary data.""")
    override def md5(context: Context, args: Arguments): Array[AnyRef] =
      if (args.count() > 1) {
        val data = simpleCost(context, args)
        val key = args.checkByteArray(1)
        hash(data, key, "MD5", "HmacMD5")
      }
      else super.md5(context, args)

    @Callback(direct = true, limit = 4, doc = """function(data:string[, hmacKey:string]):string -- Computes SHA2-256 hash of the data. Result is binary data.""")
    override def sha256(context: Context, args: Arguments): Array[AnyRef] =
      if (args.count() > 1) {
        val data = complexCost(context, args)
        val key = args.checkByteArray(1)
        hash(data, key, "SHA-256", "HmacSHA256")
      }
      else super.sha256(context, args)

    @Callback(direct = true, limit = 8, doc = """function(data:string, key: string, iv:string):string -- Encrypt data with AES. Result is binary data.""")
    def encrypt(context: Context, args: Arguments): Array[AnyRef] = crypt(context, args, Cipher.ENCRYPT_MODE)

    @Callback(direct = true, limit = 8, doc = """function(data:string, key:string, iv:string):string -- Decrypt data with AES.""")
    def decrypt(context: Context, args: Arguments): Array[AnyRef] = crypt(context, args, Cipher.DECRYPT_MODE)

    @Callback(direct = true, limit = 4, doc = """function(len:number):string -- Generates secure random binary data.""")
    def random(context: Context, args: Arguments): Array[AnyRef] = {
      val len = args.checkInteger(0)

      if (len <= 0 || len > 1024)
        throw new IllegalArgumentException("length must be in range [1..1024]")

      checkCost(Settings.get.dataCardComplex + Settings.get.dataCardComplexByte * len)
      val target = new Array[Byte](len)
      SecureRandomInstance.get.nextBytes(target)
      result(target)
    }

    // ----------------------------------------------------------------------- //

    private def crypt(context: Context, args: Arguments, mode: Int): Array[AnyRef] = {
      val data = simpleCost(context, args)

      val key = args.checkByteArray(1)
      if (key.length != 16)
        throw new IllegalArgumentException("expected a 128-bit AES key")

      val iv = args.checkByteArray(2)
      if (iv.length != 16)
        throw new IllegalArgumentException("expected a 128-bit AES IV")

      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv))
      result(cipher.doFinal(data))
    }

    private def hash(data: Array[Byte], key: Array[Byte], mode: String, hmacMode: String): Array[AnyRef] = {
      val hmac = Mac.getInstance(hmacMode)
      hmac.init(new SecretKeySpec(key, hmacMode))
      result(hmac.doFinal(data))
    }
  }

  class Tier3 extends Tier2 {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Processor,
      DeviceAttribute.Description -> "Data processor card",
      DeviceAttribute.Vendor -> "S.C. Ltd.",
      DeviceAttribute.Product -> "SC03D Signer"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, limit = 1, doc = """function([bitLen:number]):userdata, userdata -- Generates key pair. Returns: public, private keys. Allowed key lengths: 256, 384 bits.""")
    def generateKeyPair(context: Context, args: Arguments): Array[AnyRef] = {
      checkCost(Settings.get.dataCardAsymmetric)

      val bitLen = args.optInteger(0, 384)
      if (bitLen != 256 && bitLen != 384)
        throw new IllegalArgumentException("invalid key length, must be 256 or 384")

      val kpg = KeyPairGenerator.getInstance("EC")
      kpg.initialize(bitLen, SecureRandomInstance.get)
      val kp = kpg.generateKeyPair()

      result(new ECUserdata(kp.getPublic), new ECUserdata(kp.getPrivate))
    }

    @Callback(direct = true, limit = 8, doc = """function(data:string, type:string):userdata -- Restores key from its string representation.""")
    def deserializeKey(context: Context, args: Arguments): Array[AnyRef] = {
      val data = simpleCost(context, args)
      val t = args.checkString(1)

      result(new ECUserdata(ECUserdata.deserializeKey(t, data)))
    }

    @Callback(direct = true, limit = 1, doc = """function(priv:userdata, pub:userdata):string -- Generates a shared key. ecdh(a.priv, b.pub) == ecdh(b.priv, a.pub)""")
    def ecdh(context: Context, args: Arguments): Array[AnyRef] = {
      checkCost(Settings.get.dataCardAsymmetric)
      val privKey = checkUserdata(args, 0, isPublic = Option(false)).value
      val pubKey = checkUserdata(args, 1, isPublic = Option(true)).value

      val ka = KeyAgreement.getInstance("ECDH")
      ka.init(privKey)
      ka.doPhase(pubKey, true)
      result(ka.generateSecret)
    }

    @Callback(direct = true, limit = 1, doc = """function(data:string, key:userdata[, sig:string]):string or boolean -- Signs or verifies data.""")
    def ecdsa(context: Context, args: Arguments): Array[AnyRef] = {
      val data = asymmetricCost(context, args)
      val key = checkUserdata(args, 1)
      val sig = args.optByteArray(2, null)

      val sign = Signature.getInstance("SHA256withECDSA")
      if (sig != null) {
        // Verify mode
        key.value match {
          case public: PublicKey =>
            sign.initVerify(public)
            sign.update(data)
            result(sign.verify(sig))
          case _ => throw new IllegalArgumentException("public key expected")
        }
      }
      else {
        // Sign mode
        key.value match {
          case k: PrivateKey =>
            sign.initSign(k)
            sign.update(data)
            result(sign.sign())
          case _ =>
            throw new IllegalArgumentException("private key expected")
        }
      }
    }

    // ----------------------------------------------------------------------- //

    private def checkUserdata(args: Arguments, i: Int, isPublic: Option[Boolean] = None) =
      args.checkAny(i) match {
        case value: ECUserdata =>
          if (isPublic.fold(true)(_ == value.isPublic)) value
          else throw new IllegalArgumentException(
            s"${if (isPublic.get) "public" else "private"} key expected at ${i + 1}")
        case null => throw new IllegalArgumentException(
          s"bad argument #${i + 1} (userdata expected, got no value)")
        case value => throw new IllegalArgumentException(
          s"bad argument #${i + 1} (userdata expected, got ${value.getClass.getName})")
      }
  }

  class ECUserdata(var value: Key) extends prefab.AbstractValue {
    // Empty constructor for deserialization.
    def this() = this(null)

    def isPublic = value.isInstanceOf[ECPublicKey]

    def keyType = if (isPublic) ECUserdata.PublicTypeName else ECUserdata.PrivateTypeName

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():boolean -- Returns whether key is public.")
    def isPublic(context: Context, args: Arguments): Array[AnyRef] = result(isPublic)

    @Callback(direct = true, doc = "function():string -- Returns type of key.")
    def keyType(context: Context, args: Arguments): Array[AnyRef] = result(keyType)

    @Callback(direct = true, limit = 4, doc = "function():string -- Returns string representation of key. Result is binary data.")
    def serialize(context: Context, args: Arguments): Array[AnyRef] = result(value.getEncoded)

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound): Unit = {
      val keyType = nbt.getString("Type")
      val data = nbt.getByteArray("Data")
      value = ECUserdata.deserializeKey(keyType, data)
    }

    override def save(nbt: NBTTagCompound): Unit = {
      nbt.setString("Type", keyType)
      nbt.setByteArray("Data", value.getEncoded)
    }
  }

  object ECUserdata {
    final val PrivateTypeName = "ec-private"
    final val PublicTypeName = "ec-public"

    def deserializeKey(typeName: String, data: Array[Byte]): Key = {
      if (typeName == PrivateTypeName) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(data))
      else if (typeName == PublicTypeName) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data))
      else throw new IllegalArgumentException("invalid key type, must be ec-public or ec-private")
    }
  }

}