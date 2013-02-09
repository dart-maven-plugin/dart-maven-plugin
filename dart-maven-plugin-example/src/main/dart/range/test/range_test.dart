import 'package:unittest/unittest.dart';
import 'package:range/range.dart';

void main() {
  test("range() produces a list that starts at start", () {
    expect(range(0, 4)[0], equals(0));
  });

  test("range() throws an exception when start > stop", () {
    expect(() => range(5, 2), throwsA(new isInstanceOf<ArgumentError>()));
  });
//  test("fail test", () {
  //    fail("Just fail");
  //  });
}
