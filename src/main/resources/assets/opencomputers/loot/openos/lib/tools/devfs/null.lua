return
{
  open = function(mode)
    if not mode or not mode:match("[wa]") then
      return nil, "write only"
    end
    return
    {
      write = function() end
    }
  end
}
