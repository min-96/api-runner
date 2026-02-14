package com.apirunner.ide.marker

import com.apirunner.ide.psi.ControllerMethodInfo

class ControllerRunLineMarker {
    fun shouldShowRunIcon(method: ControllerMethodInfo): Boolean {
        return method.methodAnnotations.any {
            it.startsWith("@GetMapping") ||
                it.startsWith("@PostMapping") ||
                it.startsWith("@PutMapping") ||
                it.startsWith("@DeleteMapping") ||
                it.startsWith("@PatchMapping") ||
                it.startsWith("@RequestMapping")
        }
    }
}
