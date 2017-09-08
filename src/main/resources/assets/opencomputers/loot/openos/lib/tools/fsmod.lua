local filesystem = require("filesystem")

local lib = {}
function lib.remove(path, findNode)
  local function removeVirtual()
    local _, _, vnode, vrest = findNode(filesystem.path(path), false, true)
    -- vrest represents the remaining path beyond vnode
    -- vrest is nil if vnode reaches the full path
    -- thus, if vrest is NOT NIL, then we SHOULD NOT remove children nor links
    if not vrest then
      local name = filesystem.name(path)
      if vnode.children[name] or vnode.links[name] then
        vnode.children[name] = nil
        vnode.links[name] = nil
        while vnode and vnode.parent and not vnode.fs and not next(vnode.children) and not next(vnode.links) do
          vnode.parent.children[vnode.name] = nil
          vnode = vnode.parent
        end
        return true
      end
    end
    -- return false even if vrest is nil because this means it was a expected
    -- to be a real file
    return false
  end
  local function removePhysical()
    local node, rest = findNode(path)
    if node.fs and rest then
      return node.fs.remove(rest)
    end
    return false
  end
  local success = removeVirtual()
  success = removePhysical() or success -- Always run.
  if success then return true
  else return nil, "no such file or directory"
  end
end

function lib.rename(oldPath, newPath, findNode)
  if filesystem.isLink(oldPath) then
    local _, _, vnode, _ = findNode(filesystem.path(oldPath))
    local target = vnode.links[filesystem.name(oldPath)]
    local result, reason = filesystem.link(target, newPath)
    if result then
      filesystem.remove(oldPath)
    end
    return result, reason
  else
    local oldNode, oldRest = findNode(oldPath)
    local newNode, newRest = findNode(newPath)
    if oldNode.fs and oldRest and newNode.fs and newRest then
      if oldNode.fs.address == newNode.fs.address then
        return oldNode.fs.rename(oldRest, newRest)
      else
        local result, reason = filesystem.copy(oldPath, newPath)
        if result then
          return filesystem.remove(oldPath)
        else
          return nil, reason
        end
      end
    end
    return nil, "trying to read from or write to virtual directory"
  end
end

return lib
