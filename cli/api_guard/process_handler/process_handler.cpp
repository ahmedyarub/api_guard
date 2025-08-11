#include "process_handler.h"

#include <iostream>
#include <boost/asio/io_context.hpp>
#include <boost/process/v2/environment.hpp>

namespace bp = boost::process;
namespace asio = boost::asio;

std::string runProcess(const boost::process::v2::filesystem::path& command, const std::vector<std::string>& args)
{
    char buffer[10240] = {};

    {
        try
        {
            asio::io_context ctx;

            // TODO read till pipe is closed
            if (bp::popen proc(ctx.get_executor(), command, args); proc.running())
            {
                proc.read_some(asio::buffer(buffer, 10240));

                proc.wait();
            }
        }
        catch (const std::exception& e)
        {
            // std::cerr << "[Standard Exception] " << e.what() << '\n';
        }
        catch (...)
        {
            std::cerr << "[Unknown Error] Something went wrong.\n";
        }
    }

    return buffer;
}

boost::process::v2::filesystem::path getExecutablePath(const std::string& exe)
{
    return bp::environment::find_executable(exe);
}