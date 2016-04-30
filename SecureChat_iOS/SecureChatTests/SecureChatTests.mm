//
//  SecureChatTests.mm
//  SecureChatTests
//
//  Created by William Woody on 2/17/16.
//  Copyright © 2016 by William Edward Woody.
//

/*	SecureChat: A secure chat system which permits secure communications 
 *  between iOS devices and a back-end server.
 *
 *	Copyright © 2016 by William Edward Woody
 *
 *	This program is free software: you can redistribute it and/or modify it 
 *	under the terms of the GNU General Public License as published by the 
 *	Free Software Foundation, either version 3 of the License, or (at your 
 *	option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *	for more details.
 *
 *	You should have received a copy of the GNU General Public License along 
 *	with this program. If not, see <http://www.gnu.org/licenses/>
 */

#import <XCTest/XCTest.h>
#include "SCBigInteger.h"
#include "SCRSAEncryption.h"

@interface SecureChatTests : XCTestCase

@end

@implementation SecureChatTests

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

#pragma mark - Construction/Destruction

/*
 *	Simple constructor tests
 */

- (void)testInitBigInteger
{
	// Basic construction tests
	SCBigInteger bi(10);
	std::string str = bi.ToString();
	XCTAssert(str == "10");

	SCBigInteger bi2(-1280);
	str = bi2.ToString();
	XCTAssert(str == "-1280");

	SCBigInteger bi3("1024000");
	str = bi3.ToString();
	XCTAssert(str == "1024000");

	// Large value construction
	SCBigInteger bi4("12345678901234567890123456789012345678901234567890");
	str = bi4.ToString();
	XCTAssert(str == "12345678901234567890123456789012345678901234567890");

	SCBigInteger bi5("-1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
	str = bi5.ToString();
	XCTAssert(str == "-1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
}

- (void)testCompare
{
	SCBigInteger a("12345678901234567890123456789012345678901234567890");
	SCBigInteger b("12345678901234567890123456789012345678901234567891");
	SCBigInteger c("-12345678901234567890123456789012345678901234567892");

	XCTAssert(a < b);			// basic test of all compare operators
	XCTAssert(a <= a);
	XCTAssert(a <= b);
	XCTAssert(b > a);
	XCTAssert(b >= a);
	XCTAssert(a != b);
	XCTAssert(a == a);

	XCTAssert(b > c);
	XCTAssert(c < b);
	XCTAssert(b >= c);
	XCTAssert(c <= b);
	XCTAssert(c != b);
	XCTAssert(c == c);
}

- (void)testAdditionSubtraction
{
	SCBigInteger a;

	a = SCBigInteger("1234") + SCBigInteger("1111");
	XCTAssert(a.ToString() == "2345");

	a = SCBigInteger("1234") + SCBigInteger("-1111");
	XCTAssert(a.ToString() == "123");

	a = SCBigInteger("-1111") + SCBigInteger("1234");
	XCTAssert(a.ToString() == "123");

	a = SCBigInteger("4294967296") + SCBigInteger("-1");
	XCTAssert(a.ToString() == "4294967295");

	a = SCBigInteger("4294967296") - SCBigInteger("1");
	XCTAssert(a.ToString() == "4294967295");
}

- (void)testMultiplication
{
	SCBigInteger a;

	a = SCBigInteger("10") * SCBigInteger("10");
	XCTAssert(a.ToString() == "100");

	a = SCBigInteger("-10") * SCBigInteger("10");
	XCTAssert(a.ToString() == "-100");

	a = SCBigInteger("-10") * SCBigInteger("-10");
	XCTAssert(a.ToString() == "100");

	a = SCBigInteger("1000000000") * SCBigInteger("1000");
	XCTAssert(a.ToString() == "1000000000000");

	a = SCBigInteger("1000") * SCBigInteger("1000000000");
	XCTAssert(a.ToString() == "1000000000000");
}

- (void)testDivision
{
	SCBigInteger a;

	a = SCBigInteger("5") / SCBigInteger("2");
	XCTAssert(a.ToString() == "2");

	a = SCBigInteger("2") / SCBigInteger("3");
	XCTAssert(a.ToString() == "0");

	a = SCBigInteger("2") / SCBigInteger("2");
	XCTAssert(a.ToString() == "1");

	a = SCBigInteger("5") % SCBigInteger("2");
	XCTAssert(a.ToString() == "1");

	a = SCBigInteger("2") % SCBigInteger("3");
	XCTAssert(a.ToString() == "2");

	a = SCBigInteger("2") % SCBigInteger("2");
	XCTAssert(a.ToString() == "0");

	SCBigInteger b("1000000000000");
	SCBigInteger c("1000");

	printf("%s\n",b.ToString().c_str());
	printf("%s\n",c.ToString().c_str());

	a = SCBigInteger("1000000000000") / SCBigInteger("1000");
	printf("%s\n",a.ToString().c_str());

	XCTAssert(a.ToString() == "1000000000");

	a = SCBigInteger("1000000000000") / SCBigInteger("1000000000");
	XCTAssert(a.ToString() == "1000");
}

- (void)testGCD
{
	const char *data[] = {
		"7391", "3800", "19",
		"148082103", "259131054", "1443",
		"4199643119718", "26775829935540", "76158",
		"117958071441730464", "29398276097604864", "4113504",
		"8976611946440215990500", "3109435416527372283750", "110370750",
		"147990232885348896076448010", "416077461712389473971421076", "618770538",
		"7196761073972606899558049642762", "34461557662632851465700213186080", "30543731602",
		"169939728355064117154988951694261088", "269107711432599962288471037202713744", "509160354192",
		"9492817254528451457178626105824421204610", "37446756795358455104890144752914965810465", "30305802042485",
		"2168591054292748754773815698607494471364066", "1082988740600911262747817916020652608666516359", "950381187909257",
	};

	for (int i = 0; i < 10; i += 3) {
		const char *a = data[i*3];
		const char *b = data[i*3 + 1];
		const char *c = data[i*3 + 2];

		SCBigInteger aval(a);
		SCBigInteger bval(b);
		SCBigInteger cval = aval.GCD(bval);

		XCTAssert(cval.ToString() == c);
	}
}

- (void)testModInverse
{
	const char *data[] = {
		"624", "37", "22",
		"660071109", "349957702", "333134693",
		"708475234071", "245510637113", "185458439094",
		"31801498581596", "826707458754049", "818701406299447",
		"330658719722719953", "870680525041861487", "415874063524694502",
		"1043002331137170724609", "634954678145655484159", "401025403847891278868",
		"691682913539700399657123", "795556438595301353475823", "21197098883126310530438",
		"297817195293439315686244607", "626375067837512670059486743", "73326601502111066469506448",
		"1134869777830214911045862855779", "639887059261303536499048933730", "494443139992729291544836469429",
	};

	SCBigInteger aa("708475234071");
	SCBigInteger bb("245510637113");
	SCBigInteger cc = aa.ModInverse(bb);
//	printf("> %s\n",cc.ToString().c_str());
	XCTAssert(cc.ToString() == "185458439094");


	for (int i = 0; i < 9; i += 3) {
		const char *a = data[i*3];
		const char *b = data[i*3 + 1];
		const char *c = data[i*3 + 2];

		SCBigInteger aval(a);
		SCBigInteger bval(b);
		SCBigInteger cval = aval.ModInverse(bval);

		XCTAssert(cval.ToString() == c);
	}

	SCBigInteger atest("749908");
	SCBigInteger btest("462424");
	SCBigInteger ctest = atest.ModInverse(btest);
	XCTAssert(ctest.IsNan());
}

- (void)testModPow
{
	const char *data[] = {
		"298", "390", "282", "238",
		"96641", "197572", "612720", "503281",
		"615392872", "438093897", "412060034", "294178734",
		"712574345991", "386279933121", "239867459915", "133882583286",
		"785059975873290", "715078622969816", "444620013823261", "69004403597139",
		"471565102245363541", "559168519891365761", "946135741227194909", "388145446742036629",
		"34771795261795504286", "353929423429671553296", "1139168368140046805268", "58163521726320185152",
		"983892646732824683584151", "1195252142596241658140228", "1150516610002862121321638", "1000661452716684749688499",
		"210170586744002321600008077", "950245805154817385249509696", "1007273439256241562765184162", "10901762472365543986960743",
		"1031915564075369330246093649621", "308414239929437917056943837354", "63447848064035276252804363253", "43600242697665240911863539984",
	};

	for (int i = 0; i < 10; i += 3) {
		const char *a = data[i*4];
		const char *b = data[i*4 + 1];
		const char *c = data[i*4 + 2];
		const char *d = data[i*4 + 3];

		SCBigInteger aval(a);
		SCBigInteger bval(b);
		SCBigInteger cval(c);
		SCBigInteger dval = aval.ModPow(bval, cval);

		XCTAssert(dval.ToString() == d);
		if (dval.ToString() != d) {
			printf("---\n");
			printf("  Error\n");
			printf("  aval %s\n",a);
			printf("  bval %s\n",b);
			printf("  cval %s\n",c);
			printf("  dval %s\n",d);
			printf("  res  %s\n",dval.ToString().c_str());
			printf("---\n");
		}
	}
}

/*
 *	Note: because this is returning true if a number is probabilistically
 *	a prime, this test can sometimes on occassion fail
 */

- (void)testPrime
{
	SCBigInteger test("6150877");
	XCTAssert(test.IsProbablePrime());

	const char *data[] = {
		"345", "false",
		"31373", "false",
		"528576", "false",
		"6150877", "true",
		"208238722", "false",
		"1698288118574401", "true",
		"20824416365", "false",
		"1012480847860", "false",
		"31139568787278", "false",
		"422272590439601", "false",
		"24065767735107753", "false",
		"1087404305419170146", "false",
		"20551606178790309880", "false",
		"450754537567661790217", "false",
		"6878269862394782941685", "false",
		"510784572125121603259271", "true",
		"101163944730733298013190", "false",
		"741605563682189863113003777", "false",
		"13390744654824736148179399594", "false",
		"369104992626273997948360992065", "false",
	};

	for (int i = 0; i < 20; i += 3) {
		const char *a = data[i*2];
		const char *b = data[i*2 + 1];

		SCBigInteger aval(a);
		const char *str = aval.IsProbablePrime() ? "true" : "false";

		if (str != b) {
			printf("%s\n",a);
		}
		XCTAssert(str == b);
	}
}

/*
 *	Hammering testMod hard to verify it works for large numbers used in
 *	RSA encoding
 */

- (void)testMod
{
	SCBigInteger a,b,m,res;

    a = SCBigInteger("783");
    b = SCBigInteger("893");
    m = SCBigInteger("822");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "693");

    a = SCBigInteger("106803");
    b = SCBigInteger("924894");
    m = SCBigInteger("5479");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "1");

    a = SCBigInteger("584761280");
    b = SCBigInteger("935636587");
    m = SCBigInteger("383203710");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "144785300");

    a = SCBigInteger("729594564273");
    b = SCBigInteger("791151138490");
    m = SCBigInteger("836738790831");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "630731041416");

    a = SCBigInteger("262182179396888");
    b = SCBigInteger("936307626332712");
    m = SCBigInteger("816433678325739");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "674617335084262");

    a = SCBigInteger("564041100805524384");
    b = SCBigInteger("212677751610015959");
    m = SCBigInteger("29837447200259765");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "4489966916238809");

    a = SCBigInteger("1037574690856515368309");
    b = SCBigInteger("25002504697325246631");
    m = SCBigInteger("885662107345988603674");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "780884626772823371243");

    a = SCBigInteger("1052542756850401570534659");
    b = SCBigInteger("98307925025886778573134");
    m = SCBigInteger("1110917447611801191147722");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "105025593137323355309671");

    a = SCBigInteger("334448703090952160264213103");
    b = SCBigInteger("835461938428539810333899721");
    m = SCBigInteger("497527405599845512589381932");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "418494750227931101009802895");

    a = SCBigInteger("517619681538484161660858119632");
    b = SCBigInteger("549291388541512382899532723686");
    m = SCBigInteger("425232479182423144579819019427");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "225959385915523584691442187337");

    a = SCBigInteger("1146292365366720384870013476543717");
    b = SCBigInteger("276476302510568713855264414489898");
    m = SCBigInteger("979211369640075871788707247681808");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "488900771080001766949227275979961");

    a = SCBigInteger("203788653680559854504607868407031842");
    b = SCBigInteger("1004922588802846249783904167796348612");
    m = SCBigInteger("734949342074904745924774680135068762");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "23547127254395256719591408890263468");

    a = SCBigInteger("1327923869459613751725931832300525559571");
    b = SCBigInteger("246692216427009647565966903266068701199");
    m = SCBigInteger("683070614439295860628491912506250181793");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "307078120608910021752962902799524014319");

    a = SCBigInteger("412329140461562260609280569570679658636688");
    b = SCBigInteger("453676597404233314708332820743155728786034");
    m = SCBigInteger("740612754687993209909674666670656133898115");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "538742723891892349897904077223286225792224");

    a = SCBigInteger("733663849335548303635760665489896564757480800");
    b = SCBigInteger("431637425814781057597925088407262063546065046");
    m = SCBigInteger("1340040582357413765822502647889887776927883051");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "439923779711925379326547775650326395059309575");

    a = SCBigInteger("129393987883956566456873459378182359573676830746");
    b = SCBigInteger("666950350915912414699782232422056747011366498674");
    m = SCBigInteger("313597287337016381145837210865993027827002073023");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "193631128238050112897573220087155701841347649766");

    a = SCBigInteger("1148167417891829229897363283949384929794129132329163");
    b = SCBigInteger("691199462256333611508822799459218814662179833287457");
    m = SCBigInteger("216590031577542095390673801889120890947885732606839");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "73706454535542854323874959656746719413554083121302");

    a = SCBigInteger("659907208391301235165279829313836750084134901229743635");
    b = SCBigInteger("358653330772391701108943748835470592022379499996454016");
    m = SCBigInteger("211362212176843083646903659022404242383588896377339466");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "203603743479514982614358915163509275859045751013978843");

    a = SCBigInteger("740706280001551845469925412227487715806060162561006041932");
    b = SCBigInteger("1023976778336208940706036156455587674524476656728067188325");
    m = SCBigInteger("983758754141629324414329353268567262454701057627205603874");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "910360824447234272109058908468346464897315332528232340372");

    a = SCBigInteger("1358053842566613363666369743847822176553003505555871591563220");
    b = SCBigInteger("776484493859010320654076332048584799546184554436254555133454");
    m = SCBigInteger("971738267663264526231224206562931638886228401530001100813635");
    res = a.ModPow(b, m);
    XCTAssert(res.ToString() == "377840875642565972757956976758361880930705405906685234405210");

}

/*
 *	Test individual steps of RSA to verify math, using known values.
 */

- (void)testRSASkeleton
{
	SCBigInteger one = 1;
	SCBigInteger p("9257053301639623045260173518362268047555520764887304680992218013714752780706094962029227207045121430817708667545866804410231903919933835446806354411138251");
	SCBigInteger q("12647966103796762709644234834211697865802153675940549521930777654236190502785869756345728127491042929202716725576089419840309383276006115312923243593836249");

	SCBigInteger n = p * q;
	XCTAssert(n.ToString() == "117082896380177861470083805874495192709942251483549252792322442115533372272573610962532289313823030944423663439233298645149088498085690960569699829472958700269953706248851541316009747219332208157485900666315026037990307452789729163224431551111029017955173190614331462970271241225417971660326357142935794260499");

	SCBigInteger phiN = (p - one) * (q - one);
	XCTAssert(phiN.ToString() == "117082896380177861470083805874495192709942251483549252792322442115533372272573610962532289313823030944423663439233298645149088498085690960569699829472958678364934300812465786411601394645366294799811459838460823114994639501846445671259713176155694481790813170188938341014046990684130775720375597413337789286000");

	SCBigInteger e("103617663088403625989571554215607793586599431462758045438563497030907892022247612860492573488498816553006303159970293107090199421377932871253423232207779525927039571634847215354290541530880242123524248894410035065405483581504063254220556965886110130350439335928917698927791842047811971398733878772968459456621");
	XCTAssert(e.GCD(phiN) == one);
	XCTAssert(e < phiN);

	SCBigInteger d = e.ModInverse(phiN);
	XCTAssert(d.ToString() == "12558805946971939765243993824144514929678743213966228357900149755655018745992084318280273407016871829800193533894441545898374793351238299125809278171946068737287843104878551193678942780545045148270006426977714121803088244420720777153341572970609321095455276255564267662935067350676108787110712660859826283781");

	SCBigInteger tmp("36200898448447993599406251933499961126358981760098572850246469608966884091184967742536705075905236001348271316234156835597867355397158312721702430691289562001949732935728619804163781124765171635826060045326568924463426947693169446435032416605157564991362400085591559640341425291762088577580367906885306394431");
	SCBigInteger enc = tmp.ModPow(d, n);
	XCTAssert(enc.ToString() == "45770947156829890620748829519469360950922074294477537857224466939838034901193576391447493889822548934285769252637721842100820214753210409216665113256714725055729752890578675253874354187503962317447350472201667593487443122947154399728117354949072652368331524788599883203788210098980997499184952760461096406539");
	SCBigInteger dec = enc.ModPow(e, n);
	XCTAssert(dec.ToString() == "36200898448447993599406251933499961126358981760098572850246469608966884091184967742536705075905236001348271316234156835597867355397158312721702430691289562001949732935728619804163781124765171635826060045326568924463426947693169446435032416605157564991362400085591559640341425291762088577580367906885306394431");
}

- (void)testRSAKey
{
	SCRSAKey key;
	SCBigInteger res;

//	key = SCRSAKey(SCBigInteger(2),SCBigInteger(101));
//	res = key.Transform(SCBigInteger(100));
//	XCTAssert(res.ToString() == "1");		// 5**2 mod 100

	key = SCRSAKey(100,SCBigInteger(2),SCBigInteger("10000000001"));
	res = key.Transform(SCBigInteger("10000000000"));
	XCTAssert(res.ToString() == "1");		// 5**2 mod 100
}

- (void)testRSAGenerator
{
	SCRSAKey pub;
	SCRSAKey priv;

	// Hard core test: generate a 4096 public/private key, then verify that
	// we can encrypt and decrypt.
	SCRSAKeyGeneratePair(1024, pub, priv);

	SCBigInteger tmp = SCBigInteger::Random(1022);	// slightly smaller
	SCBigInteger enc = tmp.ModPow(pub.Exponent(), pub.Modulus());
	SCBigInteger dec = enc.ModPow(priv.Exponent(), priv.Modulus());

	XCTAssert(tmp == dec);

	// Test fast operators
	SCBigInteger fenc = pub.Transform(tmp);
	XCTAssert(fenc == enc);
	SCBigInteger fdec = priv.Transform(fenc);
	XCTAssert(fdec == dec);
}



@end
