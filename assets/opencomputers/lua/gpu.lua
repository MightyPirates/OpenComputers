--[[ API for graphics cards. ]]
driver.gpu = {}

function driver.gpu.setResolution(gpu, screen, w, h)
	sendToNode("gpu.setResolution", gpu, screen, w, h)
end

function driver.gpu.getResolution(gpu, screen)
	return sendToNode("gpu.getResolution", gpu, screen)
end

function driver.gpu.getResolutions(gpu, screen)
	return sendToNode("gpu.resolutions", gpu, screen)
end

function driver.gpu.set(gpu, screen, col, row, value)
	sendToNode("gpu.set", gpu, screen, col, row, value)
end

function driver.gpu.fill(gpu, screen, col, row, w, h, value)
	sendToNode("gpu.fill", gpu, screen, col, row, w, h, value:sub(1, 1))
end

function driver.gpu.copy(gpu, screen, col, row, w, h, tx, ty)
	sendToNode("gpu.copy", gpu, screen, col, row, w, h, tx, ty)
end