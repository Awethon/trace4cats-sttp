package trace4cats.sttp

import sttp.client4.GenericRequest

package object client4 {
  type SttpSpanNamer = GenericRequest[_, _] => String
}
