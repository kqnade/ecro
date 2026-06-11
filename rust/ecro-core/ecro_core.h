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

/// # Safety
///
/// `width` and `height` must be valid writable pointers to `i32` values.
int32_t ecro_get_terminal_size(int32_t *width, int32_t *height);

ecro_EcroEvent *ecro_poll_event();

ecro_EcroEvent *ecro_read_event();

/// # Safety
///
/// `event` must be either null or a pointer returned by `ecro_poll_event` or `ecro_read_event`.
void ecro_free_event(ecro_EcroEvent *event);

}  // extern "C"
