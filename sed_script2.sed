/val change = event.changes.firstOrNull()/c\
                        val change = event.changes.firstOrNull { it.pressed && !it.isConsumed }\
                        if (change != null) {\
                            val xNorm = (change.position.x / size.width).coerceIn(0f, 1f)\
                            viewModel.setDragTarget(xNorm)\
                        } else if (event.changes.none { it.pressed && !it.isConsumed }) {\
                            viewModel.clearDrag()\
                        }
