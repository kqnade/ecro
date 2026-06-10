#include <cstdarg>
#include <cstdint>
#include <cstdlib>
#include <ostream>
#include <new>

struct ecro_EcroEvent {
  int32_t event_type;
  int32_t key_code;
  int32_t modifiers;
};

extern "C" {

int32_t ecro_init();

int32_t ecro_shutdown();

int32_t ecro_enable_raw_mode();

int32_t ecro_disable_raw_mode();

int32_t ecro_enter_alternate_screen();

int32_t ecro_leave_alternate_screen();

int32_t ecro_get_terminal_size(uint16_t *width, uint16_t *height);

ecro_EcroEvent *ecro_poll_event();

void ecro_free_event(ecro_EcroEvent *event);

}  // extern "C"
